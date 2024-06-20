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

import java.awt.* ;
import java.lang.* ;
import java.io.* ;
import java.applet.* ;
import java.util.* ;
import java.text.* ;

public class Alteration
  implements Cloneable, Serializable
{
  private int index ;

  static String[] alterations = {"bb", "b", " ", "#", "X"} ;
  static Alteration alteration_bemol = new Alteration( 1 );
  static Alteration pas_d_alterations = new Alteration( 2 );
  static Alteration alteration_dieze = new Alteration( 3 );

  static int[] demi_tons_par_alteration = {-2, -1, 0, 1, 2} ;
  static Color color_selected = Color.red ;  

  // The following need special treatments

  static Alteration alteration_diminue = 
    new Alteration( alterations.length ) ;
  static int alteration_diminue_index = alteration_diminue.index ;
  static String diminue = "/" ;

  static Alteration alteration_becarre = 
    new Alteration( alteration_diminue.index+1 ) ;
  static int alteration_becarre_index = alteration_becarre.index ;
  static String becarre = "H" ;

  static class Jazz
    extends Alteration
  {
    Jazz( int i ) {
      super( i ) ;
    }
  }

  Alteration( int i ) {
    index = i ;
  }

  Alteration( StringTokenizer st ) 
    throws Exception
  {
    index = Integer.parseInt( st.nextToken()) ;    
  }

  public Object clone() {
    return (Object)new Alteration( index ) ;
  }

  public String sprintf() {
    return ""+index ;
  }
  
  static private Image selectAlteration( Color c, 
					 Image n, Image r, Image g ) {
    return ( (c == Chord.color_selected ) ? g :
	     (c == Note.color_selected ) ? r :
	     n ) ;
  }

  public void draw( Graphics g, int x, int y, int x_step ) {
    Color c = g.getColor() ;
    String as = alterations[ index ] ;

    x -= Score.x_offset_alterations*as.length() ;
    y -= Score.y_offset_alterations ;

    for( int i = 0 ; i<as.length() ; i++, x += x_step ) {
      String s = as.substring( i, i+1 ) ;

      if( s.equals( "b" )) {
	Image im = selectAlteration( c, Score.bemol, 
				     Score.bemol_r, Score.bemol_g ) ;
	g.drawImage( im, x, y+Score.y_offset_bemol, null ) ;
      }
      else if( s.equals( "#" )) {
	Image im = selectAlteration( c, Score.dieze, 
				     Score.dieze_r, Score.dieze_g ) ;
	g.drawImage( im, x, y+Score.y_offset_dieze, null ) ;
      }
      else if( s.equals( " " )) {
	Image im = selectAlteration( c, Score.becarre, 
				     Score.becarre_r, Score.becarre_g ) ;
	g.drawImage( im, x, y+Score.y_offset_dieze, null ) ;
      }
      else {
	g.drawString( s, x, y+Score.y_offset_x ) ;
      }
    }
  }

  public void drawString( Graphics g, int x, int y ) {
    String alteration = nom() ;

    x -= Score.x_offset_alterations*alteration.length() ;
    g.drawString( alteration, x, y ) ;
  }

  public boolean equals( Alteration a ) {
    return a.index == this.index ;
  }

  public String nom() {
    return (equals( pas_d_alterations ) ? "" : alterations[ index ]) ;
  }

  static String[] accidentals = {"__", "_", "=", "^", "^^"} ;
  
  public String abc() {
    return accidentals[ index ] ;
  }

  public Alteration add( int demi_tons ) 
    throws Intervalle.TooComplexException
  {
    Alteration a = new Alteration( index+demi_tons ) ;

    if( a.index < 0 || a.index >= alterations.length ) {
      String s = "(Alteration/add "+a.index+")" ;
      throw new Intervalle.TooComplexException( s ) ;
    }
    return a ;
  }

  public int indexGap( Alteration a ) {
    return index-a.index ;
  }

  public boolean lessThan( Alteration a ) {
    return index < a.index ;
  }

  public int demiTons() {
    return demi_tons_par_alteration[ index ] ;
  }

  public void save( PrintWriter st ) {
    st.print( index ) ;
  }
}
