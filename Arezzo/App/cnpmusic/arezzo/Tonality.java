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
import java.util.* ;

public class Tonality 
  extends Note 
  implements Serializable 
{
  int mode ;
  Note[] gamme ;

  private Tonality relatif ;

  static private Note[] armure ;

  static int mediante = 2 ; 

  // Intervalles en demi-tons par mode.

  static int[][] intervalles = {
    {2, 2, 1, 2, 2, 2, 1},/* majeur */

    {2, 1, 2, 2, 1, 3, 1},/* mineur harmonique */
    {2, 1, 2, 2, 2, 2, 1},/* mineur melodique ascendant */
    {2, 1, 2, 2, 1, 2, 2},/* mineur melodique descendant */
  } ;
  static int mode_majeur = 0 ;
  static int mode_mineur = 1 ;
  static int mode_mineur_ascendant = 2 ;
  static int mode_mineur_descendant = 3 ;

  static String[] modes = {"M", "m"} ;

  // Positions pour les diezes (reverse pour les bemols)

  static int[] armure_positions = {
    3, 0, 4, 1, 5, 2, 6
  } ;
  static int[][] armure_octaves_haut = {
    {4, 4, 4, 4, 3, 4, 3},/* dieze */
    {3, 4, 3, 4, 3, 4, 3} /* bemol */
  } ;
  static int[][] armure_octaves_bas = {
    {2, 2, 2, 2, 2, 2, 1},/* dieze */
    {2, 2, 1, 2, 1, 2, 1} /* bemol */
  } ;
  static int inter_alteration = 5 ;
  static int diezes_octave = 0 ;
  static int bemols_octave = 1 ;

  static {
    armure = new Note[noms.length] ;
    
    for( int i = 0 ; i<noms.length ; i++ ) {
      armure[i] = 
	new Note( armure_positions[i], 
		  armure_octaves_haut[diezes_octave][i],
		  (-1), 
		  Alteration.pas_d_alterations ) ;
    }
  }

  Tonality( Note n, int m ) 
    throws Intervalle.TooComplexException
  {
    super( n.position, n.octave, n.duree, n.alteration ) ;
    setMode( m ) ;
    setGamme() ;
    setRelatif() ;
  }

  Tonality() 
    throws Intervalle.TooComplexException
  {
    this( new Note(0, 0, 4, new Alteration( 2 )), mode_majeur ) ;
  }

  Tonality( StringTokenizer st ) 
    throws IOException, Intervalle.TooComplexException
  {
    super( st ) ;
    String t = st.nextToken() ;
    setMode( t.equals(modes[mode_majeur]) ? mode_majeur : mode_mineur ) ; 
    setGamme() ;
    setRelatif() ; 
  }

  public String nom() {
    return 
      super.nom()+
      modes[(mode>=mode_mineur) ? mode_mineur : mode_majeur] ;
  }

  String sprintf() {
    return super.sprintf()+"\n"+modes[mode] ;
  }

  boolean isUsable() {
    if( alteration.equals( Alteration.pas_d_alterations )){
      return true ;
    }
    if( alteration.lessThan( Alteration.alteration_bemol ) || 
	Alteration.alteration_dieze.lessThan( alteration )) {
      return false ;
    }
    return true ;
  }

  void setMode( int m ) {
    mode = m ;
  }

  private void setGamme() 
    throws Intervalle.TooComplexException
  {
    int nb_notes = intervalles[mode].length ;
    gamme = new Note[nb_notes] ;
    Note prev = gamme[position] = (Note)this ;
    int p = (position+1)%nb_notes ;
    for(int i = 0 ; i < nb_notes-1 ; 
	prev = gamme[p], p = (p+1)%nb_notes, i++ ) {
      gamme[p] = (Note)prev.clone() ;
      gamme[p].setPosition( p ) ;
      gamme[p].setAlteration( Alteration.pas_d_alterations ) ;
      int diff = gamme[p].demiTons()-prev.demiTons() ;
	    
      if( diff < 0 ) {
	diff += Intervalle.an_octave.demiTons() ;
      }
      int alt = intervalles[mode][i] - diff ;
      int a = 0 ;

      for( ; a < Alteration.demi_tons_par_alteration.length ; a++ ) {
	if( Alteration.demi_tons_par_alteration[a] == alt ) {
	  gamme[p].setAlteration( new Alteration( a )) ;
	  break ;
	}
      }
      if( a == Alteration.demi_tons_par_alteration.length ) {
	String s = "setGamme/Too complex tonality ("+p+")"  ;
	throw new Intervalle.TooComplexException( s ) ;
      }
    }
  }

  public void setRelatif() {
    if( mode == mode_mineur ) {
      int md = position+mediante ;
      Note med = 
	(Note)gamme[md - ((md>=noms.length)? noms.length : 0)].clone() ;
      
      try {
	relatif = new Tonality( med, mode_majeur ) ;
      }
      catch( Intervalle.TooComplexException e ) {
	Harmony.fail( "Tonality/setRelatif: "+med.nom()) ;
      }
    }
  }

  // x,y est en bas a gauche du mode a ecrire

  public void draw(Graphics g, int x, int y) {
    int alt_length = 
      Alteration.alteration_dieze.indexGap( Alteration.alteration_bemol)+1 ;
    int i_list = 
      modes.length*(position*alt_length+
		    (alteration.indexGap( Alteration.alteration_bemol)))+
      mode ;

    int y_bas = y ;
    int y_haut = y-Score.diff_y_bas_y_haut ;
    Tonality majeur = (mode==mode_mineur) ? relatif( mode_majeur ) : this ;

    if( majeur.position == do2.position && 
	majeur.alteration.equals( Alteration.pas_d_alterations )) {
      return ;
    }
    if( majeur.position == fa1.position &&
	majeur.alteration.equals( Alteration.pas_d_alterations )) {
      Note n = armure[ armure.length-1 ]  ;
      n.setAlteration( Alteration.alteration_bemol ) ;
      n.setOctave( armure_octaves_haut[bemols_octave][armure.length-1] ) ;
      Chord.drawIfPresent( g, this, x, y_haut, 
			    n, mi3, fa4, Chord.hampe_up ) ;
      n.setOctave( armure_octaves_bas[bemols_octave][armure.length-1] ) ;
      Chord.drawIfPresent( g, this, x, y_bas, n, 
			    sol1, la2, Chord.hampe_up ) ;
      return ;
    }
    if( majeur.alteration.equals( Alteration.alteration_bemol )) {
      for( int i = armure_positions.length-1 ; i>=0 ; i-- ) {
	Note n = armure[i] ;
	n.setAlteration( Alteration.alteration_bemol ) ;
	n.setOctave( armure_octaves_haut[bemols_octave][i] ) ;
	Chord.drawIfPresent( g, this, x, y_haut, 
			      n, mi3, fa4, Chord.hampe_up ) ;
	n.setOctave( armure_octaves_bas[bemols_octave][i] ) ;
	Chord.drawIfPresent( g, this, x, y_bas, 
			      n, sol1, la2, Chord.hampe_up ) ;
      
	if( i < armure_positions.length-1 &&
	    armure[i+1].position == majeur.position ) {
	  break ;
	}
	x += inter_alteration ;
      }
      return ;
    }
    for( int i = 0 ; i<armure_positions.length ; i++ ) {
      Note n = armure[i] ;
      n.setAlteration( Alteration.alteration_dieze ) ;
      n.setOctave( armure_octaves_haut[diezes_octave][i] ) ;      
      Chord.drawIfPresent( g, this, x, y_haut, n, mi3, fa4, 0 ) ;
      n.setOctave( armure_octaves_bas[diezes_octave][i] ) ;      
      Chord.drawIfPresent( g, this, x, y_bas, n, sol1, la2, 0 ) ;

      if( n.position == majeur.noteSensible().position ) {
	break ;
      }
      x += inter_alteration ;
    }
  }

  public int width () {
    Tonality majeur = 
      (mode==mode_mineur) ? relatif( mode_majeur ) : this ;
      
    if( majeur.position == do2.position &&
	majeur.alteration.equals( Alteration.pas_d_alterations )) {
      return 0 ;
    }
    if( majeur.position == fa1.position &&
	majeur.alteration.equals( Alteration.pas_d_alterations )) {
      return inter_alteration ;
    }
    if ( majeur.alteration.equals( Alteration.alteration_bemol )) {
      int w = 0 ;
      for( int i = armure_positions.length-1 ; i>=0 ; i-- ) {
	w += inter_alteration ;

	if( i < armure_positions.length-1 &&
	    armure[i+1].position == majeur.position ) {
	  break ;
	}
      }
      return w ;
    }
    for( int i = 0 ; i<armure_positions.length ; i++ ) {
      if( armure[i].position == majeur.noteSensible().position ) {
	return( (i+1)*inter_alteration ) ;
      }
    }
    Harmony.fail( "width/Unexpected "+majeur.nom()) ;
    return -1 ;
  }

  //

  public Note noteSensible() {
    int prev = (position == 0) ? noms.length-1 : position-1 ;
    return gamme[prev] ;
  }

  public Note noteTonique() {
    return gamme[position] ;
  }

  public Note noteSubdominant() {
    int subdominant = 
      (position+Note.fa2.position-Note.do2.position) % noms.length ;
    return gamme[ subdominant ] ;
  }

  public Tonality relatif( int m ) {
    if( m == mode_mineur ) {
      Harmony.fail( "relatif/Unexpected mode" ) ;
    }
    return relatif ;
  }

  public boolean contains( Note n ) {
    for( int i = 0 ; i < gamme.length ; i++ ) {
      if( n.hasSameName( gamme[i] )) {
	return true ;
	}
      }
    return false ;
  }

  public Degre degre( Note n ) {
    for( int i = 0 ; i < gamme.length ; i++ ) {
      if( n.position == gamme[ i ].position ) {
	return new Degre( i ) ;
      }
    }
    return new Degre( -1 ) ;
  }

  public Tonality alteredTonality( Chord c ) 
    throws Intervalle.TooComplexException
  { 
    if( c.degre.alteration.equals( Alteration.pas_d_alterations )) {
      return this ;
    }
    Note base = gamme[ c.degre.index ] ;

    Note altered = (Note)base.clone() ;
    Alteration a = base.alteration.add( c.degre.alteration.demiTons()) ;
    altered.setAlteration( a ) ;

    Note tierce = (Note)base.clone() ;
    tierce.increment( Intervalle.third ) ;
    int o = tierce.octave ;
    tierce = gamme[ tierce.position ] ;
    tierce.setOctave( o ) ;
    int m = 
      (new Intervalle( altered, tierce )).isTierceMajeure() ? 
      mode_majeur : 
      mode_mineur ;

    return new Tonality( altered, m ) ;
  }
}
	
