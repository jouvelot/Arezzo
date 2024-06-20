// This file is part of Arezzo.

// Arezzo is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// Arezzo is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with Foobar.  If not, see <http://www.gnu.org/licenses/>.

// Copyright (C) Pierre Jouvelot, 1997-2014, MINES ParisTech

// Contributors : Jerome Segard (2000)

package cnpmusic.arezzo ;

import cnpmusic.arezzo.* ;
import java.io.* ;
import java.awt.* ;

class Degre 
  implements Serializable 
{
  int index ;
  Alteration alteration ;

  static String[] noms = {
    "I", "II", "III", "IV", "V", "VI", "VII"
  } ;
  static Color color_selected = Color.red ;

  private static int index_deuxieme = 1 ;
  private static int index_quatrieme = 3 ;
  private static int index_sixtieme = 5 ;

  static Degre deuxieme, quatrieme, sixtieme ;

  static {
    deuxieme = new Degre( index_deuxieme ) ;
    quatrieme = new Degre( index_quatrieme ) ;
    sixtieme = new Degre( index_sixtieme ) ;
  }

  Degre() {
    this( 0 ) ;
  }

  Degre( int i ) {
    index = i ;
    alteration = Alteration.pas_d_alterations ;
  }

  Degre( Chord a, Tonality t ) {
    this() ;
    Note p = a.fondamentale( t ) ;

    if( p == null ) {
      index = -1 ;
    }
    else {
      index = p.position-t.position ;
      index += (p.position >= t.position) ? 0 : Note.noms.length ;
    }
  }

  public void draw( Graphics g, int x, int y ) {
    draw( g, x, y, nom()) ;
  }

  public void draw( Graphics g, int x, int y, Chord c, Tonality t ) {
    draw( g, x, y, jazz( c, t )) ;
  }

  private void draw( Graphics g, int x, int y, String s ) {
    Color old_color = g.getColor() ;
    g.setColor( (this == Harmony.me.score.selected_degre)?
		color_selected:old_color ) ;
    g.drawString( (index == -1) ? "*" : s, x, y ) ;
    g.setColor( old_color ) ;
  }

  public boolean equals( Degre d ) {
      return index == d.index ;
  }

  public void setAlteration( Alteration a ) {
    alteration = a ;
  }

  public String nom() {
    return (index == -1) ? "*" : alteration.nom()+noms[ index ] ;
  }

  public String jazz( Chord c, Tonality t ) {
    Note f = c.fondamentale( t ) ;

    if( f != null ) {
      setAlteration( f.alteration ) ;
      return nom()+c.chiffrage.jazzDegrePostfix( c, t ) ;
    }
    return Chiffrage.unknown_jazz ;
  }
}
