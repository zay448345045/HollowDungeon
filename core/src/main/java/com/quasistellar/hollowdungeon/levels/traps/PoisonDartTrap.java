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

package com.quasistellar.hollowdungeon.levels.traps;

import com.quasistellar.hollowdungeon.Assets;
import com.quasistellar.hollowdungeon.ShatteredPixelDungeon;
import com.quasistellar.hollowdungeon.actors.Char;
import com.quasistellar.hollowdungeon.items.weapon.missiles.darts.PoisonDart;
import com.quasistellar.hollowdungeon.sprites.MissileSprite;
import com.quasistellar.hollowdungeon.Dungeon;
import com.quasistellar.hollowdungeon.actors.Actor;
import com.quasistellar.hollowdungeon.actors.buffs.Buff;
import com.quasistellar.hollowdungeon.actors.buffs.Poison;
import com.quasistellar.hollowdungeon.mechanics.Ballistica;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class PoisonDartTrap extends Trap {

	{
		color = GREEN;
		shape = CROSSHAIR;
		
		canBeHidden = false;
	}
	
	protected int poisonAmount(){
		return 8 + Math.round(2* Dungeon.depth / 3f);
	}
	
	protected boolean canTarget( com.quasistellar.hollowdungeon.actors.Char ch ){
		return true;
	}
	
	@Override
	public void activate() {
		com.quasistellar.hollowdungeon.actors.Char target = Actor.findChar(pos);
		
		if (target != null && !canTarget(target)){
			target = null;
		}
		
		//find the closest char that can be aimed at
		if (target == null){
			for (com.quasistellar.hollowdungeon.actors.Char ch : Actor.chars()){
				Ballistica bolt = new Ballistica(pos, ch.pos, Ballistica.PROJECTILE);
				if (canTarget(ch) && bolt.collisionPos == ch.pos &&
						(target == null || Dungeon.level.trueDistance(pos, ch.pos) < Dungeon.level.trueDistance(pos, target.pos))){
					target = ch;
				}
			}
		}
		if (target != null) {
			final Char finalTarget = target;
			final PoisonDartTrap trap = this;
			if (Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[target.pos]) {
				Actor.add(new com.quasistellar.hollowdungeon.actors.Actor() {
					
					{
						//it's a visual effect, gets priority no matter what
						actPriority = VFX_PRIO;
					}
					
					@Override
					protected boolean act() {
						final com.quasistellar.hollowdungeon.actors.Actor toRemove = this;
						((com.quasistellar.hollowdungeon.sprites.MissileSprite) ShatteredPixelDungeon.scene().recycle(MissileSprite.class)).
							reset(pos, finalTarget.sprite, new PoisonDart(), new Callback() {
								@Override
								public void call() {
									int dmg = Random.NormalIntRange(4, 8);
									finalTarget.damage(dmg, trap);
									if (finalTarget == Dungeon.hero && !finalTarget.isAlive()){
										com.quasistellar.hollowdungeon.Dungeon.fail( trap.getClass() );
									}
									Buff.affect( finalTarget, Poison.class ).set( poisonAmount() );
									Sample.INSTANCE.play(Assets.Sounds.HIT, 1, 1, Random.Float(0.8f, 1.25f));
									finalTarget.sprite.bloodBurstA(finalTarget.sprite.center(), dmg);
									finalTarget.sprite.flash();
									com.quasistellar.hollowdungeon.actors.Actor.remove(toRemove);
									next();
								}
							});
						return false;
					}
				});
			} else {
				finalTarget.damage(Random.NormalIntRange(4, 8), trap);
				Buff.affect( finalTarget, Poison.class ).set( poisonAmount() );
			}
		}
	}
}
