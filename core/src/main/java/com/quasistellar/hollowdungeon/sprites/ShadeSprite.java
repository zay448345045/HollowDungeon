/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2019 Evan Debenham
 *
 * Hollow Dungeon
 * Copyright (C) 2020-2021 Pierre Schrodinger
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

package com.quasistellar.hollowdungeon.sprites;

import com.quasistellar.hollowdungeon.Assets;
import com.watabou.noosa.TextureFilm;

public class ShadeSprite extends MobSprite {

	public ShadeSprite() {
		super();
		
		texture( Assets.Sprites.SHADE );
		
		TextureFilm frames = new TextureFilm( texture, 14, 20 );

		idle = new Animation( 1, true );
		idle.frames( frames, 0, 0, 0, 1, 0, 0, 1, 1 );

		run = new Animation( 10, true );
		run.frames( frames, 2, 3, 4, 5, 6, 7 );

		die = new Animation( 5, false );
		die.frames( frames, 8, 9, 10, 11, 12, 13 );

		attack = new Animation( 15, false );
		attack.frames( frames, 3, 4, 5, 6 );
		
		play( idle );
	}
	
	@Override
	public int blood() {
		return 0x000000;
	}
}
