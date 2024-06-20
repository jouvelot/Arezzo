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

public class Note 
  implements Cloneable, Serializable 
{
  int position ;
  int octave ;
  int duree ;
  Alteration alteration ;

  private int demi_tons ;
  private int x ;
  private int y ;	

  static final int no_coordinate = -10000 ;
  static String[] noms = {"do", "re", "mi", "fa", "sol", "la", "si"} ;
  static String pas_de_nom = "rien" ;

  // Durees (-1 autorise' pour la clef).

  static int[] durees_connues = {1, 2, 4, 8, 16, 32} ;
  static int clef = -1 ;
  static int blanche = 2 ;
  static int noire = 4 ;

  static int inter_note = Score.inter_ligne/2 ;
  static int[] demi_tons_par_note = {0, 2, 4, 5, 7, 9, 11} ;
  static final int diametre_note = Score.inter_ligne ;
  static final int hampe_coefficient = 3 ;
  static int x_offset_barre = 5 ;
  static int x_barbule_offset = 6 ;
  static int y_barbule_offset = 8 ;
  static int inter_barbules = 5 ;
  static Color color_selected = Color.red ;

  public static Note sol1, fa1, 
    do2, re2b, mi2b, fa2, sol2, la2, si2b,
    do3, re3, mi3, la3, 
    do4, re4, fa4, la4 ;

  static {
    Alteration no = Alteration.pas_d_alterations ;
    Alteration bemol = Alteration.alteration_bemol ;
    
    fa1 = new Note( 3, 1, 4, no ) ;
    sol1 = new Note( 4, 1, 4, no ) ;
    do2 = new Note( 0, 2, 4, no ) ;
    re2b = new Note( 1, 2, 4, bemol ) ;
    mi2b = new Note( 2, 2, 4, bemol ) ;
    fa2 = new Note( 3, 2, 4, no ) ;
    sol2 = new Note( 4, 2, 4, no ) ;
    la2 = new Note( 5, 2, 4, no ) ;
    si2b = new Note( 6, 2, 4, bemol ) ;
    do3 = new Note( 0, 3, 4, no ) ;
    re3 = new Note( 1, 3, 4, no ) ;
    mi3 = new Note( 2, 3, 4, no ) ;
    la3 = new Note( 5, 3, 4, no ) ;
    do4 = new Note( 0, 4, 4, no ) ;
    re4 = new Note( 1, 4, 4, no ) ;
    fa4 = new Note( 3, 4, 4, no ) ;
    la4 = new Note( 5, 4, 4, no ) ;

    int y0 = Score.portee_y ;
    Note.sol1.setXY( no_coordinate, y0 ) ;
    int diff_la2_y = Intervalle.positionDifference( Note.sol1, Note.la2 ) ;
    Note.la2.setXY( no_coordinate, y0 - diff_la2_y*inter_note ) ;

    y0 -= Score.diff_y_bas_y_haut ;
    Note.mi3.setXY( no_coordinate, y0 ) ;
    int diff_fa4_y = Intervalle.positionDifference( Note.mi3, Note.fa4 ) ;
    Note.fa4.setXY( no_coordinate, y0 - diff_fa4_y*inter_note ) ;
  }

  protected Note( int p, int o, int d, Alteration a ) {
    this.position = p ;
    this.octave = o ;
    this.duree = d ;
    this.alteration = a  ;
    setDemiTons() ;
    setXY( no_coordinate, no_coordinate ) ;
  }

  Note() {
    this( 0, 0, 4, Alteration.pas_d_alterations ) ;
  }

  public Note(StringTokenizer st) {
    try {
      String s = st.nextToken() ;
      int i = 0 ;

      if( s.equals( pas_de_nom )) {
	position = -1 ;
	return ;
	}
      for( ; i < noms.length ; i++ ) {
	if( noms[i].equals( s )) {
	  this.position = i ;
	  break ;
	}
      }
      if( i == noms.length ) {
	Harmony.fail( "Note - Incorrect note name |"+s+"|" ) ;
      }
      this.octave = Integer.parseInt( st.nextToken()) ;
      this.duree = Integer.parseInt( st.nextToken()) ;
      this.alteration = new Alteration( st ) ;
    }
    catch( Exception e ) {
      Harmony.fail( "Note - Malformed file" ) ;
    }
    setDemiTons() ;
    setXY( no_coordinate, no_coordinate ) ;
  }

  public Object clone() {
    Note n = 
      new Note( position, octave, duree, (Alteration)alteration.clone()) ;
    n.setXY( x, y ) ;
    return (Object)n ;
  }

  public void setXY( int x0, int y0 ) {
    x = x0; 
    y = y0;
  }

  public void setPosition( int p ) {
    position = p ;
    setDemiTons() ;
  }

  public void setAlteration( Alteration i ) {
    alteration = i ;
    setDemiTons() ;
  }

  public void setOctave( int i ) {
    octave = i ;
    setDemiTons() ;
  }

  String sprintf() {
    return noms[position]+"\t"+octave+"\t"+duree+"\t"+alteration.sprintf() ;
  }

  //

  static byte MIDI_do0 = (byte)0x18 ;
  static byte MIDI_canal0 = (byte)0x90 ;
  static byte MIDI_velocite = (byte)0x40 ;

  public void saveMIDI( DataOutputStream st, 
			int channel, int on ) 
    throws IOException 
  {
    st.write( MIDI_canal0+channel ) ;
    st.write( demi_tons+MIDI_do0 ) ;
    st.write( on*MIDI_velocite ) ;
  }

  public static void saveEmptyMIDI( DataOutputStream st, 
				    int channel, int on ) 
    throws IOException 
  {
    st.write( MIDI_canal0+channel ) ;
    st.write( MIDI_do0 ) ;
    st.write( (byte)0x0 ) ;
  }

  //

  public String nom() {
    return 
      Harmony.me.messages.getString( noms[this.position] )+
      ((octave == 0) ? "" : "("+this.octave+")")+
      alteration.nom() ;
  }

  static String[] letters = {"c", "d", "e", "f", "g", "a", "b"} ;
  static String[] LETTERS = {"C", "D", "E", "F", "G", "A", "B"} ;

  public String lettre() {
    return letters[ position ].toUpperCase()+alteration.nom() ;
  }

  String abc( Note c, Tonality t ) {
    String letter = letters[ position ] ;
    int c_octave = c.octave ;

    if( Intervalle.positionDifference( this, c ) > 0 ) {
      letter = letter.toUpperCase() ;
      c_octave-- ;
    }
    int octave_diff = c_octave-octave ;
    String ocs = "" ;

    for( int i = octave_diff ; 
	 i!= 0 ;
	 i += ((octave_diff > 0) ? -1 : 1 )) {
      ocs += ((octave_diff > 0) ? "," : "'" ) ;
    }
    return
      (needsAlteration( t ) ? alteration.abc() : "")+
      letter+ocs+"/"+duree ;
  }

  private static final int up = 1, down = -1 ;

  public void draw( Graphics g, Tonality t, 
		    Note bottom, Note top, int hampe) {
    Color old_color = g.getColor() ;
    g.setColor( (this == Harmony.me.score.selected_note)?
		color_selected:old_color ) ;

    if( Intervalle.isAscendant( top, this)) {
      drawBarres( g, top, up ) ;
    }
    if( Intervalle.isAscendant( this, bottom )) {
      drawBarres( g, bottom, down ) ;
    }
    if( needsAlteration( t )) {
      alteration.draw( g, x, y, Score.x_offset_alterations ) ;
    }
    if( duree == clef ) {
    }
    else if( duree >= noire ) {
      g.fillOval( x, y-diametre_note/2, diametre_note, diametre_note ) ;
    }
    else {  
      g.drawOval( x, y-diametre_note/2, diametre_note, diametre_note ) ;
    }
    int hampe_x = x+((hampe == Chord.hampe_up) ? diametre_note : 0) ;

    if( duree >= blanche ) {
      int y_barbule = y-hampe_coefficient*hampe*diametre_note ;
      g.drawLine( hampe_x, y, hampe_x, y_barbule ) ;
      int d = duree ;

      for( ; d>noire ; d >>= 1, y_barbule +=inter_barbules*hampe ) {
	g.drawLine( hampe_x, y_barbule, 
		    hampe_x+x_barbule_offset, 
		    y_barbule+y_barbule_offset*hampe ) ;
      }
    }
    g.setColor( old_color ) ;
  }

  public boolean isSelected( int new_y ) {
    return Math.abs(y - new_y) < Note.diametre_note ;
  }

  public void move( int new_y ) {
    new_y = ((new_y + inter_note/2)/inter_note)*inter_note ;
    position += (y-new_y)/inter_note ;
    int direction = (position>=0) ? 1 : -1 ;

    while( position<0 || position>=noms.length ) {
      position -= direction*noms.length ;
      octave += direction ;
    }
    Tonality t = Harmony.me.score.sequence.tonality ;
    setAlteration( t.gamme[ position ].alteration ) ;
    setXY( x, new_y ) ;
  }

  public int demiTons() {
      return demi_tons ;
  }
      
  public boolean isForeign( Tonality t ) {
    for( int i = 0 ; i<t.gamme.length ; i++ ) {
      if( Constants.debug_Note ) {
	System.err.println( "isForeign:"+sprintf()+
			    ", "+t.gamme[i].sprintf()) ;
      }
      if( Intervalle.isOctave( this, t.gamme[i] )) {
	return false ;
      }
    }
    if( t.mode == Tonality.mode_mineur ) {
      try {
	return
	  isForeign( new Tonality( t, Tonality.mode_mineur_ascendant )) &&
	  isForeign( new Tonality( t, Tonality.mode_mineur_descendant )) ;
      }
      catch( Intervalle.TooComplexException e ) {
	return true ;
      }
    }
    return true ;
  }

  public boolean hasSamePitch( Note n ) {
    return hasSameName( n ) && octave == n.octave ;
  }

  public boolean hasSameName( Note n ) {
    return position == n.position && alteration.equals( n.alteration ) ;
  }

  public boolean isAltered( Note n ) {
    return position == n.position && !alteration.equals( n.alteration ) ;
  }
    
  void checkRange( String opt, Note[] range, String voice_name ) 
    throws CheckException
  {
    if( Intervalle.isAscendant( this, range[0] )) {
      String s = Harmony.me.messages.getString( "Accord.TooLow" ) ;
      MessageFormat mf = new MessageFormat( s ) ;
      String ms = mf.format( new String[] {voice_name}) ;
      throw new CheckException( opt, ms ) ;
    }
    if( Intervalle.isAscendant( range[1], this )) {
      String s = Harmony.me.messages.getString( "Accord.TooHigh" ) ;
      MessageFormat mf = new MessageFormat( s ) ;
      String ms = mf.format( new String[] {voice_name}) ;
      throw new CheckException( opt, ms ) ;
    }
  }

  public Note add( Intervalle i ) 
    throws Intervalle.TooComplexException
  {
    Note n = (Note)this.clone() ;

    if( i.isOctave()) {
      Harmony.fail( "Note/add: adding octave to "+sprintf()) ;
    }
    n.increment( i.positionDifference()) ;
    n.alteration = n.alteration.add( i.demiTons()-(n.demiTons()-demiTons())) ;
    n.setDemiTons() ;
    return n ;
  }

  public void increment( int position_diff ) {
    position += position_diff ;

    for( ; position >= noms.length ; position -= noms.length ) {
      octave ++ ;
    }
    setDemiTons() ;
  }

  //

  private void setDemiTons() {
    demi_tons = 
      octave*Intervalle.an_octave.demiTons()+
      demi_tons_par_note[position]+
      alteration.demiTons() ;
  }

  private void drawBarres( Graphics g, Note ref, int direction ) {
    int d = Intervalle.positionDistance( ref, this )/2 ;
    int barre_y = ref.y-direction*Score.inter_ligne ;

    for( ; d>0 ; d--, barre_y-=direction*Score.inter_ligne ) {
      g.drawLine( x-x_offset_barre, barre_y,
		  x+diametre_note+x_offset_barre, barre_y ) ;
    }
  }

  private boolean needsAlteration( Tonality t ) {
    Tonality t_majeur = t ;

    if( t.mode != Tonality.mode_majeur) {
      t_majeur = t.relatif( Tonality.mode_majeur ) ;
    }
    return 
      !alteration.equals( t_majeur.gamme[position].alteration ) || 
      duree == clef ;
  }
}

