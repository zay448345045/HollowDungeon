/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2019 Evan Debenham
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

import com.quasistellar.hollowdungeon.Assets;
import com.quasistellar.hollowdungeon.actors.buffs.Adrenaline;
import com.quasistellar.hollowdungeon.actors.buffs.Corruption;
import com.quasistellar.hollowdungeon.effects.Beam;
import com.quasistellar.hollowdungeon.effects.Pushing;
import com.quasistellar.hollowdungeon.items.Item;
import com.quasistellar.hollowdungeon.scenes.GameScene;
import com.quasistellar.hollowdungeon.sprites.NecromancerSprite;
import com.quasistellar.hollowdungeon.sprites.SkeletonSprite;
import com.quasistellar.hollowdungeon.Dungeon;
import com.quasistellar.hollowdungeon.actors.Actor;
import com.quasistellar.hollowdungeon.actors.Char;
import com.quasistellar.hollowdungeon.actors.buffs.Buff;
import com.quasistellar.hollowdungeon.effects.CellEmitter;
import com.quasistellar.hollowdungeon.effects.Speck;
import com.quasistellar.hollowdungeon.items.potions.PotionOfHealing;
import com.quasistellar.hollowdungeon.items.scrolls.ScrollOfTeleportation;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Necromancer extends com.quasistellar.hollowdungeon.actors.mobs.Mob {
	
	{
		spriteClass = NecromancerSprite.class;
		
		HP = HT = 35;
		
		loot = new PotionOfHealing();
		lootChance = 0.2f; //see createloot
		
		properties.add(Char.Property.UNDEAD);
		
		HUNTING = new Hunting();
	}
	
	public boolean summoning = false;
	private Emitter summoningEmitter = null;
	private int summoningPos = -1;
	
	private boolean firstSummon = true;
	
	private NecroSkeleton mySkeleton;
	private int storedSkeletonID = -1;
	
	@Override
	public void updateSpriteState() {
		super.updateSpriteState();
		
		if (summoning && summoningEmitter == null){
			summoningEmitter = CellEmitter.get( summoningPos );
			summoningEmitter.pour(Speck.factory(Speck.RATTLE), 0.2f);
			sprite.zap( summoningPos );
		}
	}

	@Override
	public void rollToDropLoot() {
		lootChance *= ((6f - Dungeon.LimitedDrops.NECRO_HP.count) / 6f);
		super.rollToDropLoot();
	}
	
	@Override
	protected Item createLoot(){
		Dungeon.LimitedDrops.NECRO_HP.count++;
		return super.createLoot();
	}
	
	@Override
	public void die(Object cause) {
		if (storedSkeletonID != -1){
			com.quasistellar.hollowdungeon.actors.Actor ch = Actor.findById(storedSkeletonID);
			storedSkeletonID = -1;
			if (ch instanceof NecroSkeleton){
				mySkeleton = (NecroSkeleton) ch;
			}
		}
		
		if (mySkeleton != null && mySkeleton.isAlive()){
			mySkeleton.die(null);
		}
		
		if (summoningEmitter != null){
			summoningEmitter.killAndErase();
			summoningEmitter = null;
		}
		
		super.die(cause);
	}
	
	private static final String SUMMONING = "summoning";
	private static final String FIRST_SUMMON = "first_summon";
	private static final String SUMMONING_POS = "summoning_pos";
	private static final String MY_SKELETON = "my_skeleton";
	
	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put( SUMMONING, summoning );
		bundle.put( FIRST_SUMMON, firstSummon );
		if (summoning){
			bundle.put( SUMMONING_POS, summoningPos);
		}
		if (mySkeleton != null){
			bundle.put( MY_SKELETON, mySkeleton.id() );
		}
	}
	
	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		summoning = bundle.getBoolean( SUMMONING );
		if (bundle.contains(FIRST_SUMMON)) firstSummon = bundle.getBoolean(FIRST_SUMMON);
		if (summoning){
			summoningPos = bundle.getInt( SUMMONING_POS );
		}
		if (bundle.contains( MY_SKELETON )){
			storedSkeletonID = bundle.getInt( MY_SKELETON );
		}
	}
	
	public void onZapComplete(){
		if (mySkeleton == null || mySkeleton.sprite == null || !mySkeleton.isAlive()){
			return;
		}
		
		//heal skeleton first
		if (mySkeleton.HP < mySkeleton.HT){

			if (sprite.visible || mySkeleton.sprite.visible) {
				sprite.parent.add(new com.quasistellar.hollowdungeon.effects.Beam.HealthRay(sprite.center(), mySkeleton.sprite.center()));
			}
			
			mySkeleton.HP = Math.min(mySkeleton.HP + 5, mySkeleton.HT);
			mySkeleton.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 1 );
			
			//otherwise give it adrenaline
		} else if (mySkeleton.buff(com.quasistellar.hollowdungeon.actors.buffs.Adrenaline.class) == null) {

			if (sprite.visible || mySkeleton.sprite.visible) {
				sprite.parent.add(new Beam.HealthRay(sprite.center(), mySkeleton.sprite.center()));
			}
			
			Buff.affect(mySkeleton, com.quasistellar.hollowdungeon.actors.buffs.Adrenaline.class, 3f);
		}
		
		next();
	}
	
	private class Hunting extends Mob.Hunting {
		
		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			enemySeen = enemyInFOV;
			
			if (storedSkeletonID != -1){
				com.quasistellar.hollowdungeon.actors.Actor ch = Actor.findById(storedSkeletonID);
				storedSkeletonID = -1;
				if (ch instanceof NecroSkeleton){
					mySkeleton = (NecroSkeleton) ch;
				}
			}
			
			if (summoning){
				
				//push anything on summoning spot away, to the furthest valid cell
				if (Actor.findChar(summoningPos) != null) {
					int pushPos = pos;
					for (int c : PathFinder.NEIGHBOURS8) {
						if (Actor.findChar(summoningPos + c) == null
								&& Dungeon.level.passable[summoningPos + c]
								&& Dungeon.level.trueDistance(pos, summoningPos + c) > Dungeon.level.trueDistance(pos, pushPos)) {
							pushPos = summoningPos + c;
						}
					}
					
					//push enemy, or wait a turn if there is no valid pushing position
					if (pushPos != pos) {
						com.quasistellar.hollowdungeon.actors.Char ch = Actor.findChar(summoningPos);
						Actor.addDelayed( new Pushing( ch, ch.pos, pushPos ), -1 );
						
						ch.pos = pushPos;
						Dungeon.level.occupyCell(ch );
						
					} else {
						spend(Actor.TICK);
						return true;
					}
				}
				
				summoning = firstSummon = false;
				
				mySkeleton = new NecroSkeleton();
				mySkeleton.pos = summoningPos;
				GameScene.add( mySkeleton );
				Dungeon.level.occupyCell( mySkeleton );
				Sample.INSTANCE.play(Assets.Sounds.BONES);
				summoningEmitter.burst( Speck.factory( Speck.RATTLE ), 5 );
				sprite.idle();
				
				if (buff(com.quasistellar.hollowdungeon.actors.buffs.Corruption.class) != null){
					com.quasistellar.hollowdungeon.actors.buffs.Buff.affect(mySkeleton, Corruption.class);
				}
				
				spend(Actor.TICK);
				return true;
			}
			
			if (mySkeleton != null &&
					(!mySkeleton.isAlive()
					|| !Dungeon.level.mobs.contains(mySkeleton)
					|| mySkeleton.alignment != alignment)){
				mySkeleton = null;
			}
			
			//if enemy is seen, and enemy is within range, and we haven no skeleton, summon a skeleton!
			if (enemySeen && Dungeon.level.distance(pos, enemy.pos) <= 4 && mySkeleton == null){
				
				summoningPos = -1;
				for (int c : PathFinder.NEIGHBOURS8){
					if (Actor.findChar(enemy.pos+c) == null
							&& Dungeon.level.passable[enemy.pos+c]
							&& fieldOfView[enemy.pos+c]
							&& Dungeon.level.trueDistance(pos, enemy.pos+c) < Dungeon.level.trueDistance(pos, summoningPos)){
						summoningPos = enemy.pos+c;
					}
				}
				
				if (summoningPos != -1){
					
					summoning = true;
					summoningEmitter = com.quasistellar.hollowdungeon.effects.CellEmitter.get(summoningPos);
					summoningEmitter.pour(Speck.factory(com.quasistellar.hollowdungeon.effects.Speck.RATTLE), 0.2f);
					
					sprite.zap( summoningPos );
					
					spend( firstSummon ? Actor.TICK : 2* Actor.TICK );
				} else {
					//wait for a turn
					spend(Actor.TICK);
				}
				
				return true;
			//otherwise, if enemy is seen, and we have a skeleton...
			} else if (enemySeen && mySkeleton != null){
				
				target = enemy.pos;
				spend(Actor.TICK);
				
				if (!fieldOfView[mySkeleton.pos]){
					
					//if the skeleton is not next to the enemy
					//teleport them to the closest spot next to the enemy that can be seen
					if (!Dungeon.level.adjacent(mySkeleton.pos, enemy.pos)){
						int telePos = -1;
						for (int c : PathFinder.NEIGHBOURS8){
							if (com.quasistellar.hollowdungeon.actors.Actor.findChar(enemy.pos+c) == null
									&& Dungeon.level.passable[enemy.pos+c]
									&& fieldOfView[enemy.pos+c]
									&& Dungeon.level.trueDistance(pos, enemy.pos+c) < com.quasistellar.hollowdungeon.Dungeon.level.trueDistance(pos, telePos)){
								telePos = enemy.pos+c;
							}
						}
						
						if (telePos != -1){
							
							ScrollOfTeleportation.appear(mySkeleton, telePos);
							mySkeleton.teleportSpend();
							
							if (sprite != null && sprite.visible){
								sprite.zap(telePos);
								return false;
							} else {
								onZapComplete();
							}
						}
					}
					
					return true;
					
				} else {
					
					//zap skeleton
					if (mySkeleton.HP < mySkeleton.HT || mySkeleton.buff(Adrenaline.class) == null) {
						if (sprite != null && sprite.visible){
							sprite.zap(mySkeleton.pos);
							return false;
						} else {
							onZapComplete();
						}
					}
					
				}
				
				return true;
				
			//otherwise, default to regular hunting behaviour
			} else {
				return super.act(enemyInFOV, justAlerted);
			}
		}
	}
	
	public static class NecroSkeleton extends Skeleton {
		
		{
			state = WANDERING;
			
			spriteClass = NecroSkeletonSprite.class;

			//20/25 health to start
			HP = 20;
		}

		@Override
		public float spawningWeight() {
			return 0;
		}

		private void teleportSpend(){
			spend(com.quasistellar.hollowdungeon.actors.Actor.TICK);
		}
		
		public static class NecroSkeletonSprite extends SkeletonSprite {
			
			public NecroSkeletonSprite(){
				super();
				brightness(0.75f);
			}
			
			@Override
			public void resetColor() {
				super.resetColor();
				brightness(0.75f);
			}
		}
		
	}
}
