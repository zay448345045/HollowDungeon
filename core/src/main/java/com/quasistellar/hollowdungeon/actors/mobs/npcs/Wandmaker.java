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

package com.quasistellar.hollowdungeon.actors.mobs.npcs;

import com.quasistellar.hollowdungeon.items.Item;
import com.quasistellar.hollowdungeon.levels.Level;
import com.quasistellar.hollowdungeon.levels.rooms.special.MassGraveRoom;
import com.quasistellar.hollowdungeon.levels.rooms.special.RotGardenRoom;
import com.quasistellar.hollowdungeon.levels.rooms.standard.RitualSiteRoom;
import com.quasistellar.hollowdungeon.plants.Rotberry;
import com.quasistellar.hollowdungeon.sprites.WandmakerSprite;
import com.quasistellar.hollowdungeon.windows.WndQuest;
import com.quasistellar.hollowdungeon.windows.WndWandmaker;
import com.quasistellar.hollowdungeon.Dungeon;
import com.quasistellar.hollowdungeon.actors.Char;
import com.quasistellar.hollowdungeon.journal.Notes;
import com.quasistellar.hollowdungeon.scenes.GameScene;
import com.quasistellar.hollowdungeon.actors.buffs.Buff;
import com.quasistellar.hollowdungeon.items.quest.CeremonialCandle;
import com.quasistellar.hollowdungeon.items.quest.CorpseDust;
import com.quasistellar.hollowdungeon.items.quest.Embers;
import com.quasistellar.hollowdungeon.levels.rooms.Room;
import com.quasistellar.hollowdungeon.messages.Messages;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Wandmaker extends NPC {

	{
		spriteClass = WandmakerSprite.class;

		properties.add(Char.Property.IMMOVABLE);
	}
	
	@Override
	protected boolean act() {
		throwItem();
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
	}
	
	@Override
	public void add( Buff buff ) {
	}
	
	@Override
	public boolean reset() {
		return true;
	}
	
	@Override
	public boolean interact(com.quasistellar.hollowdungeon.actors.Char c) {
		sprite.turnTo( pos, Dungeon.hero.pos );

		if (c != Dungeon.hero){
			return true;
		}

		if (Quest.given) {
			
			Item item;
			switch (Quest.type) {
				case 1:
				default:
					item = Dungeon.hero.belongings.getItem(CorpseDust.class);
					break;
				case 2:
					item = Dungeon.hero.belongings.getItem(Embers.class);
					break;
				case 3:
					item = Dungeon.hero.belongings.getItem(Rotberry.Seed.class);
					break;
			}

			if (item != null) {
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show( new WndWandmaker( Wandmaker.this, item ) );
					}
				});
			} else {
				String msg;
				switch(Quest.type){
					case 1: default:
						msg = Messages.get(this, "reminder_dust", Dungeon.hero.name());
						break;
					case 2:
						msg = Messages.get(this, "reminder_ember", Dungeon.hero.name());
						break;
					case 3:
						msg = Messages.get(this, "reminder_berry", Dungeon.hero.name());
						break;
				}
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new com.quasistellar.hollowdungeon.windows.WndQuest(Wandmaker.this, msg));
					}
				});
			}
			
		} else {

			String msg1 = "";
			String msg2 = "";
			switch(Dungeon.hero.heroClass){
				case KNIGHT:
					msg1 += Messages.get(this, "intro_warrior");
					break;
				case HORNET:
					msg1 += Messages.get(this, "intro_huntress");
					break;
			}

			msg1 += Messages.get(this, "intro_1");

			switch (Quest.type){
				case 1:
					msg2 += Messages.get(this, "intro_dust");
					break;
				case 2:
					msg2 += Messages.get(this, "intro_ember");
					break;
				case 3:
					msg2 += Messages.get(this, "intro_berry");
					break;
			}

			msg2 += Messages.get(this, "intro_2");
			final String msg1Final = msg1;
			final String msg2Final = msg2;
			
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new com.quasistellar.hollowdungeon.windows.WndQuest(Wandmaker.this, msg1Final){
						@Override
						public void hide() {
							super.hide();
							com.quasistellar.hollowdungeon.scenes.GameScene.show(new WndQuest(Wandmaker.this, msg2Final));
						}
					});
				}
			});

			Notes.add( Notes.Landmark.WANDMAKER );
			Quest.given = true;
		}

		return true;
	}
	
	public static class Quest {

		private static int type;
		// 1 = corpse dust quest
		// 2 = elemental embers quest
		// 3 = rotberry quest
		
		private static boolean spawned;
		
		private static boolean given;

		public static void reset() {
			spawned = false;
			type = 0;
		}
		
		private static final String NODE		= "wandmaker";
		
		private static final String SPAWNED		= "spawned";
		private static final String TYPE		= "type";
		private static final String GIVEN		= "given";
		private static final String WAND1		= "wand1";
		private static final String WAND2		= "wand2";

		private static final String RITUALPOS	= "ritualpos";
		
		public static void storeInBundle( Bundle bundle ) {
			
			Bundle node = new Bundle();
			
			node.put( SPAWNED, spawned );
			
			if (spawned) {
				
				node.put( TYPE, type );
				
				node.put( GIVEN, given );

				if (type == 2){
					node.put( RITUALPOS, CeremonialCandle.ritualPos );
				}

			}
			
			bundle.put( NODE, node );
		}
		
		public static void restoreFromBundle( Bundle bundle ) {

			Bundle node = bundle.getBundle( NODE );
			
			if (!node.isNull() && (spawned = node.getBoolean( SPAWNED ))) {

				type = node.getInt(TYPE);
				
				given = node.getBoolean( GIVEN );

				if (type == 2){
					CeremonialCandle.ritualPos = node.getInt( RITUALPOS );
				}

			} else {
				reset();
			}
		}
		
		private static boolean questRoomSpawned;
		
		public static void spawnWandmaker(Level level, Room room ) {
			if (questRoomSpawned) {
				
				questRoomSpawned = false;
				
				Wandmaker npc = new Wandmaker();
				boolean validPos;
				//Do not spawn wandmaker on the entrance, or in front of a door.
				do {
					validPos = true;
					npc.pos = level.pointToCell(room.random());
					if (npc.pos == level.entrance){
						validPos = false;
					}
					for (Point door : room.connected.values()){
						if (level.trueDistance( npc.pos, level.pointToCell( door ) ) <= 1){
							validPos = false;
						}
					}
				} while (!validPos);
				level.mobs.add( npc );

				spawned = true;

				given = false;
				
			}
		}
		
		public static ArrayList<Room> spawnRoom( ArrayList<Room> rooms) {
//			questRoomSpawned = false;
//			if (!spawned && (type != 0 || (Dungeon.depth > 6 && Random.Int( 10 - com.quasistellar.hollowdungeon.Dungeon.depth ) == 0))) {
//
//				// decide between 1,2, or 3 for quest type.
//				if (type == 0) type = Random.Int(3)+1;
//
//				switch (type){
//					case 1: default:
//						rooms.add(new MassGraveRoom());
//						break;
//					case 2:
//						rooms.add(new RitualSiteRoom());
//						break;
//					case 3:
//						rooms.add(new RotGardenRoom());
//						break;
//				}
//
//				questRoomSpawned = true;
//
//			}
			return rooms;
		}
		
		public static void complete() {
			
			Notes.remove( com.quasistellar.hollowdungeon.journal.Notes.Landmark.WANDMAKER );
		}
	}
}
