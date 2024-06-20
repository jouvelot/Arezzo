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
import java.text.* ;
import java.util.* ;

class Intervalle
{
  Note low ;
  Note high ;

  // Parameters

  int name ;
  int kind ;

  // Names

  static int second = 1 ;
  static int third = 2 ;
  static int fourth = 3 ;
  static int fifth = 4 ;
  static int sixth = 5 ;
  static int seventh = 6 ;
  static int octave = 0 ;

  static int intervalles_number = 7 ;
  
  // Kinds

  static int diminished = 0 ;
  static int minor = 1 ;
  static int just = 2 ;
  static int major = 3 ;
  static int augmented = 4 ;

  // Demi_tons indexed by names and kinds

  private static int[][] demi_tons = {
    {11, -1, 12, -1, 13},
    {0, 1, -1, 2, 3},
    {2, 3, -1, 4, 5},
    {4, -1, 5, -1, 6},
    {6, -1, 7, -1, 8},
    {7, 8, -1, 9, 10},
    {9, 10, -1, 11, 12}
  } ;

  static Intervalle an_octave = new Intervalle( octave, just ) ;

  Intervalle( int n, int k ) {
    name = n ;
    kind = k ;
  }

  static class TooComplexException
    extends Exception 
  {
    TooComplexException() {
      super() ;
    }

    TooComplexException( String s ) {
      super( s ) ;
    }

    public String throwMe() 
      throws TooComplexException
    {
      throw this ;
    }
  }

  Intervalle( Note fst, Note snd ) 
    throws TooComplexException
  {
    low = fst ;
    high = snd ;
    name = getName( low, high ) ;
    kind = getKind( low, high ) ;
  }

  static String sprintf( Note fst, Note snd ) {
    return "("+fst.sprintf()+" "+snd.sprintf()+")" ;
  }

  static int getName( Note f, Note s ) {
    if( isAscendant( f, s )) {
      return positionDifference( f, s ) % intervalles_number ;
    }
    else {
      return getName( s, f ) ;
    }
  }

  static int getKind( Note f, Note s ) 
    throws TooComplexException
  {
    if( isAscendant( f, s )) {
      int name = getName( f, s ) ;
      int diff = (s.demiTons() - f.demiTons()) % demi_tons[octave][just] ;

      for( int i = 0 ; i < demi_tons[name].length ; i++ ) {
	if( (demi_tons[name][i] % demi_tons[octave][just]) == diff ) {
	  return i ;
	}
      }
      throw new TooComplexException( sprintf( f, s )) ;
    }
    else {
      return getKind( s, f ) ;
    }
  }

  public int positionDifference() {return name ;}

  public int demiTons() {return demi_tons[name][kind];}

  public int alteration( int m ) {
    if( m == kind ) {
      return 0 ;
    }
    int step = (m-kind)/Math.abs( m-kind ) ;
    int demi = 0 ;

    for( int k = kind ; m != kind ; m -= step ) {
      demi += (demi_tons[ name ][ k ] == -1 ) ? 0 : step ;
      k += step ;
    }
    return( demi ) ;
  }

  // Melodic intervalles

  public boolean isSeconde() {return name == second ;}

  public boolean isSecondeMineure() {
    return isSeconde() && kind == minor ;
  }

  public boolean isTierce() {return name == third ;}

  public boolean isTierceMineure() {
    return isTierce() && kind == minor ;
  }

  public boolean isTierceMajeure() {
    return isTierce() && kind == major ;
  }

  public boolean isQuarte() {return name == fourth ;}

  public boolean isQuinte() {return name == fifth ;}

  public boolean isSixte() {return name == sixth ;}

  public boolean isSeptieme() {return name == seventh ;}

  public boolean isSeptiemeMineure() {
    return isSeptieme() && kind == minor ;
  }

  public boolean isSeptiemeMajeure() {
    return isSeptieme() && kind == major ;
  }

  public boolean isOctave() {return name == octave ;}

  public boolean isAscendant() {
    return Intervalle.isAscendant( low, high ) ;
  }

  public boolean isRepetition() {return isOctave() ;}

  public boolean isMelodic() {
    return !Intervalle.isLargerThanOctave( low, high ) ;
  }

  public boolean isAugmented() 
    throws TooComplexException
  {
    return isAugmented( low, high ) ;
  }

  public boolean isDiminished() 
    throws TooComplexException
  {
    return isDiminished( low, high ) ;
  }

  public String nom() {return Intervalle.nom( low, high ) ;}

  public int direction() {
    return (isRepetition()) ? 0 : (isAscendant()) ? +1 : -1 ;
  }

  public boolean isForeignSecond( Tonality t ) {
    if( t.mode == Tonality.mode_majeur ) {
      return false ;
    }
    try {
      t = new Tonality( t, (isAscendant()) ? 
			Tonality.mode_mineur_ascendant : 
			Tonality.mode_mineur_descendant ) ;
    }
    catch( Intervalle.TooComplexException e ) {
      return false ;
    }
    return low.isForeign( t ) || high.isForeign( t ) ;
  }

  // Harmonic intervalles

  static public boolean isQuinte( Note fst, Note snd ) {
    return isQuinteJuste( fst, snd ) ;
  }

  static public boolean isQuinteJuste( Note fst, Note snd ) {
    try {
      return getName( fst, snd ) == fifth && getKind( fst, snd ) == just ;
    }
    catch( TooComplexException e ) {
      return false ;
    }
  }

  static public boolean isQuarte( Note fst, Note snd ) {
    return isQuarteJuste( fst, snd ) ;
  }

  static public boolean isQuarteJuste( Note fst, Note snd ) {
    try {
      return getName( fst, snd ) == fourth && getKind( fst, snd ) == just ;
    }
    catch( TooComplexException e ) {
      return false ;
    }
  }

  static public boolean isOctave( Note fst, Note snd ) {
    try {
      return getName( fst, snd ) == octave && getKind( fst, snd ) == just ;
    }
    catch( TooComplexException e ) {
      return false ;
    }
  }

  static public boolean isLargerThanOctave( Note fst, Note snd ) {
    return positionDistance( fst, snd ) > intervalles_number ;
  }

  static public boolean isAscendant( Note fst, Note snd ) {
    return fst.demiTons() <= snd.demiTons() ;
  }

  static public int direction( Note fst, Note snd ) {
    return 
      (fst.hasSamePitch( snd )) ? 0 : 
      (isAscendant( fst, snd )) ? +1 : -1 ;
  }

  static public boolean isAugmented( Note fst, Note snd ) 
    throws TooComplexException
  {
    return isAltered( fst, snd, 1 ) ;
  }

  static public boolean isDiminished( Note fst, Note snd ) 
    throws TooComplexException
  {
    return isAltered( fst, snd, -1 ) ;
  }

  static String nom( Note fst, Note snd ) {
    ResourceBundle msgs = Harmony.me.messages ;
    String s = msgs.getString( "Intervalle.i"+
			       Math.abs( positionDifference( fst, snd ) % 
					 intervalles_number)) ;
    String msg = "" ;

    try {
      msg = 
	isAugmented( fst, snd ) ? msgs.getString( "Intervalle.Augmented" ) : 
	isDiminished( fst, snd ) ? msgs.getString( "Intervalle.Diminished" ) : 
	"" ;
    }
    catch( Intervalle.TooComplexException e ) {
    }
    return 
      new MessageFormat( s ).format( new String[] {msg})+
      " ("+fst.nom()+", "+snd.nom()+")" ;
  }

  //

  static private boolean isAltered( Note fst, Note snd, int direction ) 
    throws TooComplexException
  {
    int diff = positionDifference( fst, snd ) ;

    if( diff < 0 ) {
      return isAltered( snd, fst, -direction ) ;
    }
    Note n = (Note)fst.clone() ;
    n.increment( diff ) ;
    Tonality t = new Tonality( fst, Tonality.mode_majeur ) ;
    int offset = direction ;

    if( diff == second || diff == third || diff == sixth || diff == seventh ) {
      offset += (direction == -1 ) ? direction : 0 ;
    }
    Alteration new_alt = t.gamme[ n.position ].alteration.add( offset ) ;
    n.setAlteration( new_alt ) ;

    if( Constants.debug_Intervalle ) {
      System.err.println( "Intervalle/isAltered "+offset+
			  sprintf( fst, snd )+
			  ", aug: "+n.nom()) ;
    }
    return snd.hasSamePitch( n ) ;
  }

  static int positionDifference( Note fst, Note snd ) {
    int diff = 
      snd.position + snd.octave*Note.noms.length - 
      (fst.position + fst.octave*Note.noms.length) ;

    if( Constants.debug_Intervalle ) {
      System.err.println( "Intervalle/positionDifference: "+
			  sprintf( fst, snd )+" = "+diff ) ;
    }
    return diff ;
  }

  static int positionDistance( Note fst, Note snd ) {
    return Math.abs( positionDifference( fst, snd )) ;
  }
}
