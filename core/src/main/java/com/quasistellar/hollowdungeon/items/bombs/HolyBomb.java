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

package com.quasistellar.hollowdungeon.items.bombs;

import com.quasistellar.hollowdungeon.Assets;
import com.quasistellar.hollowdungeon.actors.Actor;
import com.quasistellar.hollowdungeon.effects.Flare;
import com.quasistellar.hollowdungeon.effects.particles.ShadowParticle;
import com.quasistellar.hollowdungeon.sprites.ItemSpriteSheet;
import com.quasistellar.hollowdungeon.tiles.DungeonTilemap;
import com.quasistellar.hollowdungeon.utils.BArray;
import com.quasistellar.hollowdungeon.Dungeon;
import com.quasistellar.hollowdungeon.actors.Char;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class HolyBomb extends Bomb {
	
	{
		image = ItemSpriteSheet.HOLY_BOMB;
	}
	
	@Override
	public void explode(int cell) {
		super.explode(cell);
		
		if (Dungeon.level.heroFOV[cell]) {
			new Flare(10, 64).show(Dungeon.hero.sprite.parent, DungeonTilemap.tileCenterToWorld(cell), 2f);
		}
		
		ArrayList<com.quasistellar.hollowdungeon.actors.Char> affected = new ArrayList<>();
		
		PathFinder.buildDistanceMap( cell, BArray.not( Dungeon.level.solid, null ), 2 );
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE) {
				com.quasistellar.hollowdungeon.actors.Char ch = Actor.findChar(i);
				if (ch != null) {
					affected.add(ch);
					
				}
			}
		}
		
		for (com.quasistellar.hollowdungeon.actors.Char ch : affected){
			if (ch.properties().contains(Char.Property.UNDEAD) || ch.properties().contains(com.quasistellar.hollowdungeon.actors.Char.Property.DEMONIC)){
				ch.sprite.emitter().start( ShadowParticle.UP, 0.05f, 10 );
				
				//bomb deals an additional 67% damage to unholy enemies in a 5x5 range
				int damage = Math.round(Random.NormalIntRange( Dungeon.depth+5, 10 + com.quasistellar.hollowdungeon.Dungeon.depth * 2 ) * 0.67f);
				ch.damage(damage, this);
			}
		}
		
		Sample.INSTANCE.play( Assets.Sounds.READ );
	}
	
	@Override
	public int price() {
		//prices of ingredients
		return quantity * (20 + 30);
	}
}
