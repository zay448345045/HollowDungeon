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

package com.quasistellar.hollowdungeon.items.spells;

import com.quasistellar.hollowdungeon.Assets;
import com.quasistellar.hollowdungeon.Dungeon;
import com.quasistellar.hollowdungeon.actors.hero.Hero;
import com.quasistellar.hollowdungeon.items.scrolls.ScrollOfMagicMapping;
import com.quasistellar.hollowdungeon.items.scrolls.ScrollOfRecharging;
import com.quasistellar.hollowdungeon.levels.traps.Trap;
import com.quasistellar.hollowdungeon.sprites.ItemSprite;
import com.quasistellar.hollowdungeon.sprites.ItemSpriteSheet;
import com.quasistellar.hollowdungeon.utils.GLog;
import com.quasistellar.hollowdungeon.items.quest.MetalShard;
import com.quasistellar.hollowdungeon.mechanics.Ballistica;
import com.quasistellar.hollowdungeon.messages.Messages;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

public class ReclaimTrap extends TargetedSpell {
	
	{
		image = ItemSpriteSheet.RECLAIM_TRAP;
	}
	
	private Class<?extends com.quasistellar.hollowdungeon.levels.traps.Trap> storedTrap = null;
	
	@Override
	protected void affectTarget(Ballistica bolt, com.quasistellar.hollowdungeon.actors.hero.Hero hero) {
		if (storedTrap == null) {
			quantity++; //storing a trap doesn't consume the spell
			com.quasistellar.hollowdungeon.levels.traps.Trap t = Dungeon.level.traps.get(bolt.collisionPos);
			if (t != null && t.active && t.visible) {
				t.disarm();
				
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
				ScrollOfRecharging.charge(hero);
				storedTrap = t.getClass();
				
			} else {
				GLog.w(Messages.get(this, "no_trap"));
			}
		} else {
			
			Trap t = Reflection.newInstance(storedTrap);
			storedTrap = null;
			
			t.pos = bolt.collisionPos;
			t.activate();
			
		}
	}
	
	@Override
	public String desc() {
		String desc = super.desc();
		if (storedTrap != null){
			desc += "\n\n" + Messages.get(this, "desc_trap", Messages.get(storedTrap, "name"));
		}
		return desc;
	}
	
	@Override
	protected void onThrow(int cell) {
		storedTrap = null;
		super.onThrow(cell);
	}
	
	@Override
	public void doDrop(Hero hero) {
		storedTrap = null;
		super.doDrop(hero);
	}
	
	private static final com.quasistellar.hollowdungeon.sprites.ItemSprite.Glowing[] COLORS = new com.quasistellar.hollowdungeon.sprites.ItemSprite.Glowing[]{
			new com.quasistellar.hollowdungeon.sprites.ItemSprite.Glowing( 0xFF0000 ),
			new com.quasistellar.hollowdungeon.sprites.ItemSprite.Glowing( 0xFF8000 ),
			new com.quasistellar.hollowdungeon.sprites.ItemSprite.Glowing( 0xFFFF00 ),
			new com.quasistellar.hollowdungeon.sprites.ItemSprite.Glowing( 0x00FF00 ),
			new com.quasistellar.hollowdungeon.sprites.ItemSprite.Glowing( 0x00FFFF ),
			new com.quasistellar.hollowdungeon.sprites.ItemSprite.Glowing( 0x8000FF ),
			new com.quasistellar.hollowdungeon.sprites.ItemSprite.Glowing( 0xFFFFFF ),
			new com.quasistellar.hollowdungeon.sprites.ItemSprite.Glowing( 0x808080 ),
			new com.quasistellar.hollowdungeon.sprites.ItemSprite.Glowing( 0x000000 )
	};
	
	@Override
	public ItemSprite.Glowing glowing() {
		if (storedTrap != null){
			return COLORS[Reflection.newInstance(storedTrap).color];
		}
		return null;
	}
	
	@Override
	public int price() {
		//prices of ingredients, divided by output quantity
		return Math.round(quantity * ((40 + 100) / 3f));
	}
	
	private static final String STORED_TRAP = "stored_trap";
	
	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(STORED_TRAP, storedTrap);
	}
	
	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		storedTrap = bundle.getClass(STORED_TRAP);
	}
	
	public static class Recipe extends com.quasistellar.hollowdungeon.items.Recipe.SimpleRecipe {
		
		{
			inputs =  new Class[]{ScrollOfMagicMapping.class, MetalShard.class};
			inQuantity = new int[]{1, 1};
			
			cost = 6;
			
			output = ReclaimTrap.class;
			outQuantity = 3;
		}
		
	}
	
}
