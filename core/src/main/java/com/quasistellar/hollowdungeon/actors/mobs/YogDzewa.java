/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2020 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.quasistellar.hollowdungeon.actors.mobs;

import com.quasistellar.hollowdungeon.actors.buffs.Light;
import com.quasistellar.hollowdungeon.effects.Beam;
import com.quasistellar.hollowdungeon.effects.Pushing;
import com.quasistellar.hollowdungeon.effects.TargetedCell;
import com.quasistellar.hollowdungeon.effects.particles.PurpleParticle;
import com.quasistellar.hollowdungeon.sprites.CharSprite;
import com.quasistellar.hollowdungeon.sprites.LarvaSprite;
import com.quasistellar.hollowdungeon.sprites.YogSprite;
import com.quasistellar.hollowdungeon.tiles.DungeonTilemap;
import com.quasistellar.hollowdungeon.Dungeon;
import com.quasistellar.hollowdungeon.Statistics;
import com.quasistellar.hollowdungeon.actors.Actor;
import com.quasistellar.hollowdungeon.actors.Char;
import com.quasistellar.hollowdungeon.effects.CellEmitter;
import com.quasistellar.hollowdungeon.effects.particles.ShadowParticle;
import com.quasistellar.hollowdungeon.scenes.GameScene;
import com.quasistellar.hollowdungeon.ui.BossHealthBar;
import com.quasistellar.hollowdungeon.utils.GLog;
import com.quasistellar.hollowdungeon.items.artifacts.DriedRose;
import com.quasistellar.hollowdungeon.mechanics.Ballistica;
import com.quasistellar.hollowdungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class YogDzewa extends com.quasistellar.hollowdungeon.actors.mobs.Mob {

	{
		spriteClass = YogSprite.class;

		HP = HT = 1000;

		//so that allies can attack it. States are never actually used.
		state = HUNTING;

		properties.add(Char.Property.BOSS);
		properties.add(Char.Property.IMMOVABLE);
		properties.add(Char.Property.DEMONIC);
	}

	private int phase = 0;

	private float abilityCooldown;
	private static final int MIN_ABILITY_CD = 10;
	private static final int MAX_ABILITY_CD = 15;

	private float summonCooldown;
	private static final int MIN_SUMMON_CD = 10;
	private static final int MAX_SUMMON_CD = 15;

	private ArrayList<Class> fistSummons = new ArrayList<>();
	{
		Random.pushGenerator(Dungeon.seedCurDepth());
			fistSummons.add(Random.Int(2) == 0 ? YogFist.BurningFist.class : YogFist.SoiledFist.class);
			fistSummons.add(Random.Int(2) == 0 ? YogFist.RottingFist.class : YogFist.RustedFist.class);
			fistSummons.add(Random.Int(2) == 0 ? YogFist.BrightFist.class : YogFist.DarkFist.class);
			Random.shuffle(fistSummons);
		Random.popGenerator();
	}

	private static final int SUMMON_DECK_SIZE = 4;
	private ArrayList<Class> regularSummons = new ArrayList<>();
	{
		for (int i = 0; i < SUMMON_DECK_SIZE; i++){
			if (i >= Statistics.spawnersAlive){
				regularSummons.add(Larva.class);
			} else {
				regularSummons.add(YogRipper.class);
			}
		}
		Random.shuffle(regularSummons);
	}

	private ArrayList<Integer> targetedCells = new ArrayList<>();

	@Override
	protected boolean act() {
		enemySeen = true;

		if (phase == 0){
			if (Dungeon.hero.viewDistance >= Dungeon.level.distance(pos, Dungeon.hero.pos)) {
				Dungeon.observe();
			}
			if (Dungeon.level.heroFOV[pos]) {
				notice();
			}
		}

		if (phase == 4 && findFist() == null){
			yell(Messages.get(this, "hope"));
			summonCooldown = -15; //summon a burst of minions!
			phase = 5;
		}

		if (phase == 0){
			spend(Actor.TICK);
			return true;
		} else {

			boolean terrainAffected = false;
			HashSet<com.quasistellar.hollowdungeon.actors.Char> affected = new HashSet<>();
			//delay fire on a rooted hero
			if (!Dungeon.hero.rooted) {
				for (int i : targetedCells) {
					Ballistica b = new Ballistica(pos, i, Ballistica.WONT_STOP);
					//shoot beams
					sprite.parent.add(new Beam.DeathRay(sprite.center(), DungeonTilemap.raisedTileCenterToWorld(b.collisionPos)));
					for (int p : b.path) {
						com.quasistellar.hollowdungeon.actors.Char ch = Actor.findChar(p);
						if (ch != null && (ch.alignment != alignment || ch instanceof Bee)) {
							affected.add(ch);
						}
						if (Dungeon.level.flamable[p]) {
							Dungeon.level.destroy(p);
							GameScene.updateMap(p);
							terrainAffected = true;
						}
					}
				}
				if (terrainAffected) {
					Dungeon.observe();
				}
				for (com.quasistellar.hollowdungeon.actors.Char ch : affected) {
					ch.damage(2, new Eye.DeathGaze());

					if (Dungeon.level.heroFOV[pos]) {
						ch.sprite.flash();
						CellEmitter.center(pos).burst(PurpleParticle.BURST, Random.IntRange(1, 2));
					}
					if (!ch.isAlive() && ch == Dungeon.hero) {
						Dungeon.fail(getClass());
						GLog.n(Messages.get(com.quasistellar.hollowdungeon.actors.Char.class, "kill", name()));
					}
				}
				targetedCells.clear();
			}

			if (abilityCooldown <= 0){

				int beams = 1 + (HT - HP)/400;
				HashSet<Integer> affectedCells = new HashSet<>();
				for (int i = 0; i < beams; i++){

					int targetPos = Dungeon.hero.pos;
					if (i != 0){
						do {
							targetPos = Dungeon.hero.pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
						} while (Dungeon.level.trueDistance(pos, Dungeon.hero.pos)
								> Dungeon.level.trueDistance(pos, targetPos));
					}
					targetedCells.add(targetPos);
					Ballistica b = new Ballistica(pos, targetPos, Ballistica.WONT_STOP);
					affectedCells.addAll(b.path);
				}

				//remove one beam if multiple shots would cause every cell next to the hero to be targeted
				boolean allAdjTargeted = true;
				for (int i : PathFinder.NEIGHBOURS9){
					if (!affectedCells.contains(Dungeon.hero.pos + i) && Dungeon.level.passable[Dungeon.hero.pos + i]){
						allAdjTargeted = false;
						break;
					}
				}
				if (allAdjTargeted){
					targetedCells.remove(targetedCells.size()-1);
				}
				for (int i : targetedCells){
					Ballistica b = new Ballistica(pos, i, Ballistica.WONT_STOP);
					for (int p : b.path){
						sprite.parent.add(new TargetedCell(p, 0xFF0000));
						affectedCells.add(p);
					}
				}

				//don't want to overly punish players with slow move or attack speed
				spend(GameMath.gate(Actor.TICK, Dungeon.hero.cooldown(), 3* Actor.TICK));
				Dungeon.hero.interrupt();

				abilityCooldown += Random.NormalFloat(MIN_ABILITY_CD, MAX_ABILITY_CD);
				abilityCooldown -= (phase - 1);

			} else {
				spend(Actor.TICK);
			}

			while (summonCooldown <= 0){

				Class<?extends com.quasistellar.hollowdungeon.actors.mobs.Mob> cls = regularSummons.remove(0);
				com.quasistellar.hollowdungeon.actors.mobs.Mob summon = Reflection.newInstance(cls);
				regularSummons.add(cls);

				int spawnPos = -1;
				for (int i : PathFinder.NEIGHBOURS8){
					if (Actor.findChar(pos+i) == null){
						if (spawnPos == -1 || Dungeon.level.trueDistance(Dungeon.hero.pos, spawnPos) > Dungeon.level.trueDistance(Dungeon.hero.pos, pos+i)){
							spawnPos = pos + i;
						}
					}
				}

				if (spawnPos != -1) {
					summon.pos = spawnPos;
					GameScene.add( summon );
					Actor.addDelayed( new com.quasistellar.hollowdungeon.effects.Pushing( summon, pos, summon.pos ), -1 );
					summon.beckon(Dungeon.hero.pos);

					summonCooldown += Random.NormalFloat(MIN_SUMMON_CD, MAX_SUMMON_CD);
					summonCooldown -= (phase - 1);
					if (findFist() != null){
						summonCooldown += MIN_SUMMON_CD - (phase - 1);
					}
				} else {
					break;
				}
			}

		}

		if (summonCooldown > 0) summonCooldown--;
		if (abilityCooldown > 0) abilityCooldown--;

		//extra fast abilities and summons at the final 100 HP
		if (phase == 5 && abilityCooldown > 2){
			abilityCooldown = 2;
		}
		if (phase == 5 && summonCooldown > 3){
			summonCooldown = 3;
		}

		return true;
	}

	@Override
	public boolean isAlive() {
		return super.isAlive() || phase != 5;
	}

	@Override
	public boolean isInvulnerable(Class effect) {
		return phase == 0 || findFist() != null;
	}

	@Override
	public void damage( int dmg, Object src ) {

		int preHP = HP;
		super.damage( dmg, src );

		if (phase == 0 || findFist() != null) return;

		if (phase < 4) {
			HP = Math.max(HP, HT - 300 * phase);
		} else if (phase == 4) {
			HP = Math.max(HP, 100);
		}
		int dmgTaken = preHP - HP;

		if (dmgTaken > 0) {
			abilityCooldown -= dmgTaken / 10f;
			summonCooldown -= dmgTaken / 10f;
		}

		if (phase < 4 && HP <= HT - 300*phase){

			Dungeon.level.viewDistance = Math.max(1, Dungeon.level.viewDistance-1);
			if (Dungeon.hero.buff(com.quasistellar.hollowdungeon.actors.buffs.Light.class) == null){
				Dungeon.hero.viewDistance = Dungeon.level.viewDistance;
			}
			Dungeon.observe();
			com.quasistellar.hollowdungeon.utils.GLog.n(Messages.get(this, "darkness"));
			sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "invulnerable"));

			YogFist fist = (YogFist) Reflection.newInstance(fistSummons.remove(0));
			fist.pos = Dungeon.level.exit;

			CellEmitter.get(Dungeon.level.exit-1).burst(ShadowParticle.UP, 25);
			CellEmitter.get(Dungeon.level.exit).burst(ShadowParticle.UP, 100);
			com.quasistellar.hollowdungeon.effects.CellEmitter.get(Dungeon.level.exit+1).burst(com.quasistellar.hollowdungeon.effects.particles.ShadowParticle.UP, 25);

			if (abilityCooldown < 5) abilityCooldown = 5;
			if (summonCooldown < 5) summonCooldown = 5;

			int targetPos = Dungeon.level.exit + Dungeon.level.width();
			if (Actor.findChar(targetPos) == null){
				fist.pos = targetPos;
			} else if (Actor.findChar(targetPos-1) == null){
				fist.pos = targetPos-1;
			} else if (Actor.findChar(targetPos+1) == null){
				fist.pos = targetPos+1;
			}

			GameScene.add(fist, 4);
			Actor.addDelayed( new Pushing( fist, Dungeon.level.exit, fist.pos ), -1 );
			phase++;
		}

		com.quasistellar.hollowdungeon.actors.buffs.LockedFloor lock = Dungeon.hero.buff(com.quasistellar.hollowdungeon.actors.buffs.LockedFloor.class);
		if (lock != null) lock.addTime(dmgTaken);

	}

	private YogFist findFist(){
		for ( com.quasistellar.hollowdungeon.actors.Char c : Actor.chars() ){
			if (c instanceof YogFist){
				return (YogFist) c;
			}
		}
		return null;
	}

	@Override
	public void beckon( int cell ) {
	}

	@Override
	public void aggro(com.quasistellar.hollowdungeon.actors.Char ch) {
		for (com.quasistellar.hollowdungeon.actors.mobs.Mob mob : (Iterable<com.quasistellar.hollowdungeon.actors.mobs.Mob>) Dungeon.level.mobs.clone()) {
			if (Dungeon.level.distance(pos, mob.pos) <= 4 &&
					(mob instanceof Larva || mob instanceof com.quasistellar.hollowdungeon.actors.mobs.RipperDemon)) {
				mob.aggro(ch);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void die( Object cause ) {

		for (com.quasistellar.hollowdungeon.actors.mobs.Mob mob : (Iterable<com.quasistellar.hollowdungeon.actors.mobs.Mob>) Dungeon.level.mobs.clone()) {
			if (mob instanceof Larva || mob instanceof com.quasistellar.hollowdungeon.actors.mobs.RipperDemon) {
				mob.die( cause );
			}
		}

		Dungeon.level.viewDistance = 4;
		if (Dungeon.hero.buff(com.quasistellar.hollowdungeon.actors.buffs.Light.class) == null){
			Dungeon.hero.viewDistance = Dungeon.level.viewDistance;
		}

		com.quasistellar.hollowdungeon.scenes.GameScene.bossSlain();
		com.quasistellar.hollowdungeon.Dungeon.level.unseal();
		super.die( cause );

		yell( Messages.get(this, "defeated") );
	}

	@Override
	public void notice() {
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss(this);
			yell(Messages.get(this, "notice"));
			for (com.quasistellar.hollowdungeon.actors.Char ch : com.quasistellar.hollowdungeon.actors.Actor.chars()){
				if (ch instanceof DriedRose.GhostHero){
					((DriedRose.GhostHero) ch).sayBoss();
				}
			}
			if (phase == 0) {
				phase = 1;
				summonCooldown = Random.NormalFloat(MIN_SUMMON_CD, MAX_SUMMON_CD);
				abilityCooldown = Random.NormalFloat(MIN_ABILITY_CD, MAX_ABILITY_CD);
			}
		}
	}

	@Override
	public String description() {
		String desc = super.description();

		if (com.quasistellar.hollowdungeon.Statistics.spawnersAlive > 0){
			desc += "\n\n" + Messages.get(this, "desc_spawners");
		}

		return desc;
	}

	{
		immunities.add( com.quasistellar.hollowdungeon.actors.buffs.Terror.class );
		immunities.add( com.quasistellar.hollowdungeon.actors.buffs.Amok.class );
		immunities.add( com.quasistellar.hollowdungeon.actors.buffs.Charm.class );
		immunities.add( com.quasistellar.hollowdungeon.actors.buffs.Sleep.class );
		immunities.add( com.quasistellar.hollowdungeon.actors.buffs.Vertigo.class );
	}

	private static final String PHASE = "phase";

	private static final String ABILITY_CD = "ability_cd";
	private static final String SUMMON_CD = "summon_cd";

	private static final String FIST_SUMMONS = "fist_summons";
	private static final String REGULAR_SUMMONS = "regular_summons";

	private static final String TARGETED_CELLS = "targeted_cells";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(PHASE, phase);

		bundle.put(ABILITY_CD, abilityCooldown);
		bundle.put(SUMMON_CD, summonCooldown);

		bundle.put(FIST_SUMMONS, fistSummons.toArray(new Class[0]));
		bundle.put(REGULAR_SUMMONS, regularSummons.toArray(new Class[0]));

		int[] bundleArr = new int[targetedCells.size()];
		for (int i = 0; i < targetedCells.size(); i++){
			bundleArr[i] = targetedCells.get(i);
		}
		bundle.put(TARGETED_CELLS, bundleArr);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		phase = bundle.getInt(PHASE);
		if (phase != 0) com.quasistellar.hollowdungeon.ui.BossHealthBar.assignBoss(this);

		abilityCooldown = bundle.getFloat(ABILITY_CD);
		summonCooldown = bundle.getFloat(SUMMON_CD);

		fistSummons.clear();
		Collections.addAll(fistSummons, bundle.getClassArray(FIST_SUMMONS));
		regularSummons.clear();
		Collections.addAll(regularSummons, bundle.getClassArray(REGULAR_SUMMONS));

		for (int i : bundle.getIntArray(TARGETED_CELLS)){
			targetedCells.add(i);
		}
	}

	public static class Larva extends Mob {

		{
			spriteClass = LarvaSprite.class;

			HP = HT = 20;
			viewDistance = Light.DISTANCE;

			properties.add(Char.Property.DEMONIC);
		}

	}

	//used so death to yog's ripper demons have their own rankings description and are more aggro
	public static class YogRipper extends RipperDemon {
	}
}
