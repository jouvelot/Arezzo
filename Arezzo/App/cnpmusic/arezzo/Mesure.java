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
import java.util.* ;
import java.net.* ;
import java.io.* ;

class Mesure 
  implements Serializable 
{
  int count ;
  int beat ;

  private int max ;            /* de Note.durees_connues (= 32ths) */
  private int current_count = 0 ;

  Mesure( int c, int b ) {
    update( c, b ) ;
  }

  Mesure() {
    this( 4, Note.noire ) ;
  }

  Mesure( StringTokenizer st ) 
    throws IOException 
  {
    String t = st.nextToken() ;
    Mesure m = Mesure.parse( t ) ;
    update( m.count, m.beat ) ;
  }

  public Object clone() {
    Mesure m = new Mesure( count, beat ) ;
    return (Object)m ;
  }

  String sprintf() {
    return count+"/"+beat ;
  }

  public static Mesure parse( String s ) {
    int c, b ;
    StringTokenizer st = new StringTokenizer( s, "/" ) ;
    try {
      c = Integer.parseInt( st.nextToken()) ;
      b = Integer.parseInt( st.nextToken()) ;
    }
    catch( NumberFormatException e ) {
      String ms = Harmony.me.messages.getString( "Mesure.Incorrect" ) ;
      Harmony.me.inform( ms+" : "+s ) ;
      return null ;
    }
    catch( NoSuchElementException e ) {
      String ms = Harmony.me.messages.getString( "Mesure.Incorrect" ) ;
      Harmony.me.inform( ms+" : "+s ) ;
      return null ;
    }
    return new Mesure( c, b ) ;
  }

  public void draw( Graphics g, int x, int y ) {
    int y_bas = y ;
    int y_haut = y-Score.diff_y_bas_y_haut ;

    g.drawString( Integer.toString( beat ), x, y_bas ) ;
    g.drawString( Integer.toString( count ), 
		  x, y_bas-Score.inter_mesure ) ;

    g.drawString( Integer.toString( beat ), x, y_haut ) ;
    g.drawString( Integer.toString( count ), 
		  x, y_haut-Score.inter_mesure ) ;
  }

  private int mesure_width = 20 ;

  public int width() {
    return mesure_width ;
  }

  public void resetCounter() {
    current_count = 0 ;
  }

  public void update( int c, int b ) {
    int d = 0 ;
    for( ; d < Note.durees_connues.length ; d++ ) {
      if( Note.durees_connues[d] == b ) {
	break ;
      }
    }
    if( d == Note.durees_connues.length || c <= 0 ) {
      String s = Harmony.me.messages.getString( "Mesure.Incorrect" ) ;
      Harmony.me.inform( s ) ;
      return ;
    }
    count = c ;
    beat = b ;
    max = c*(Note.durees_connues[Note.durees_connues.length-1]/b) ; ;
    resetCounter() ;
  }

  public void addCounter( int duree ) {
    current_count += Note.durees_connues[Note.durees_connues.length-1]/duree ;
  }

  public boolean isCompleted() {
    return current_count % max == 0 ;
  }

  public int barCount() {
    return current_count/max ;
  }
}
