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
import java.io.* ;
import java.text.* ;
import java.util.* ;

class Chord 
  implements Cloneable, Serializable, Constants
{
  Note notes[] ;
  Chiffrage chiffrage ;
  Degre degre ;

  static final int basse = 0 ;
  static final int tenor = 1 ;
  static final int alto = 2 ;
  static final int soprano = 3 ;

  static final int[] voices = new int[] {basse, tenor, alto, soprano} ;
  static final int notes_per_chord = voices.length ;

  static final int hampe_down = -1, hampe_up = 1 ;
  static Color color_selected = 
    new Color( 101, 161, 107 ) ; // ClickNPlayMusic dark green

  static Note[][] range ;
    
  static Options midi_play_options ;
  static Options midi_instrument_options ;
  static byte midi_GMpiano = 0 ;

  static String[] Voice_Names ;

  static {
    range = new Note[notes_per_chord][2] ;
    range[basse][0] = Note.fa1 ;
    range[basse][1] = Note.re3 ;
    range[tenor][0] = Note.do2 ;
    range[tenor][1] = Note.la3 ;
    range[alto][0] = Note.fa2 ;
    range[alto][1] = Note.re4 ;
    range[soprano][0] = Note.do3 ;
    range[soprano][1] = Note.la4 ;

    Voice_Names = new String[] {
      "Accord.VoiceName"+Chord.basse,
      "Accord.VoiceName"+Chord.tenor,
      "Accord.VoiceName"+Chord.alto,
      "Accord.VoiceName"+Chord.soprano
    } ;    
    midi_play_options = 
      new Options( Chord.Voice_Names, Options.option_true ) ;
  }

  Chord() {
    notes = new Note[notes_per_chord] ;
    notes[basse] = (Note)Note.sol1.clone() ;
    chiffrage = new Chiffrage() ;
    degre = new Degre() ;
  }

  Chord( StringTokenizer st, Tonality t ) {
    this() ;
    chiffrage = new Chiffrage( st ) ;
    
    for( int i = 0 ; i<notes_per_chord ; i++ ) {
      Note n = new Note( st ) ;
      notes[i] = (n.position < 0) ? null : n ;
    }
    setDegre( t ) ;
  }

  String sprintf() {
    String s = chiffrage.sprintf() ;
    	
    for( int i = 0 ; i < notes.length ; i++ ) {
	s += (( notes[i] == null ) ? 
	      Note.pas_de_nom : notes[i].sprintf())+"\n" ;
    }
    return s ;
  }

  public void setChiffrage( int i ) {
    chiffrage = new Chiffrage( i ) ;
  }

  public void setChiffrage( Chiffrage c ) {
    chiffrage = c ;
  }

  public void setDegre( Tonality t ) {
    setDegre( t, Alteration.pas_d_alterations ) ;
  }

  public void setDegre( Tonality t, Alteration a ) {
    degre = new Degre( this, t ) ;
    degre.setAlteration( a ) ;
  }

  public void setDuree( int d ) {
    for( int i = 0 ; i<notes_per_chord ; i++ ) {
      if( notes[i] != null ) {
	notes[i].duree = d ;
      }
    }
  }

  private Note[] default_inserted_notes = {
    Note.sol1, Note.la2, Note.mi3, Note.fa4
  } ;

  public Note addNoteInVoice( int voix ) {
    Note n = (Note)default_inserted_notes[voix].clone() ;
    int duree = Note.noire ;

    for( int i = 0 ; i < notes.length ; i++ ) {
      if( notes[i] != null ) {
	duree = notes[i].duree ;
      }
    }
    n.duree = duree ;
    notes[voix] = n ;
    return n ;
  }

  static byte[] delta_time = {0, 0} ;
  static int noire_ticks = 480 ;

  public void saveMIDI( DataOutputStream st ) throws IOException  {
    int chord_time = (noire_ticks*Note.noire)/notes[0].duree ;
    delta_time[0] = (byte)(((chord_time >>> 7) & 0x7f) | 0x80) ;
    delta_time[1] = (byte)(chord_time & 0x7f) ;

    for( int on = 1 ; on >=0 ; on-- ) {
      for( int i = 0 ; i < notes.length ; i++ ) {
	Note n = notes[i] ;
	      
	// Generate non-zero delta time on first OFF note
	if( on == 0 && i == 0 ) {
	  st.write( delta_time, 0, delta_time.length ) ;
	}
	else {
	  st.write( 0 ) ;
	}
	if( n == null || midi_play_options.isFalse( Voice_Names[i] )) {
	  Note.saveEmptyMIDI( st, i, on ) ;
	}
	else {
	  n.saveMIDI( st, i, on ) ;
	}
      }
    }
  }

  public int voiceIndex( Note n ) {
    for( int i = 0 ; i < notes.length ; i++ ) {
      if( n == notes[i] ) {
	return i ;
      }
    }
    Harmony.fail( "Chord/voiceIndex"+n ) ;
    return 0 ;
  }

  public Note selectedNote( int y ) {
    for( int i = 0 ; i < notes.length ; i++ ) {
      Note n = notes[i] ;

      if( n != null && n.isSelected(y)) {
	return n ;
      }
    }
    return null ;
  }

  public int duree() {
    for( int i = 0 ; i < notes.length ; i++ )
      if( notes[i] != null ) {
	return notes[i].duree ;
      }
    Harmony.fail( "Chord/duree: Empty chord " ) ;
    return 0 ;
  }

  public boolean removeNote( Note n ) {
    boolean is_empty = true ;
    
    for( int i = 0 ; i < notes.length ; i++ ) {
      if( notes[i] == n ) {
	notes[i] = Harmony.me.score.selected_note = null ;
      }
      is_empty &= (notes[i] == null) ;
    }
    return is_empty ;
  }

  public Note insertLowerNote() {
    for( int i = 0 ; i < notes.length ; i++ ) {
      if( notes[i] == null ) {
	  return addNoteInVoice( i ) ;
      }
    }
    return null ;
  }

  public Note fondamentale( Tonality t ) {
    return chiffrage.fondamentale( this, t ) ;
  }

  // x, y = position en bas (hauteur de la portee du bas) a gauche 
  // de l'accord 

  public void draw( Graphics g, Tonality t, int x, int y ) {
    Color old_color = g.getColor() ;
    g.setColor( (this == Harmony.me.score.selected_chord)?
		color_selected:old_color ) ;

    if( Score.display_options.isTrue( "Score.ShowChiffrage" )) {
      if( Score.display_options.isTrue( "Score.ShowClassic" )) {
	chiffrage.draw( g, x, y+Score.chiffrage_y ) ;
      }
      if( Score.display_options.isTrue( "Score.ShowJazz" )) {
	int y_jazz =
	  y+
	  Score.chiffrage_jazz_y+
	  ((((x-Sequence.chord_start_x)/Score.inter_chord) % 2 == 0) ? 0 :
	   Score.inter_chiffres) ;
	String s = chiffrage.jazz( this, t ) ;

	if( Score.display_options.isFalse( "Score.ShowAllJazzInversions" ) &&
	     s.indexOf( "/" ) != -1 ) {
	  s = s.substring( 0, s.indexOf( "/" )) ;
	}
	g.drawString( s, x, y_jazz ) ;
      }
    }
    if( Score.display_options.isTrue( "Score.ShowDegre" )) {
      if( Score.display_options.isTrue( "Score.ShowJazz" )) {
	degre.draw( g, x, y-Score.degre_y, this, t ) ;
      }
      else {
	degre.draw( g, x, y-Score.degre_y ) ;
      }
    }
    drawIfPresent( g, t, x, y, notes[basse], 
		   Note.sol1, Note.la2, hampe_down ) ;
    drawIfPresent( g, t, x, y, notes[tenor], 
		   Note.sol1, Note.la2, hampe_up ) ;

    y -= Score.diff_y_bas_y_haut ;
    drawIfPresent( g, t, x, y, 
		   notes[alto], Note.mi3, Note.fa4, hampe_down ) ;
    drawIfPresent( g, t, x, y, 
		   notes[soprano], Note.mi3, Note.fa4, hampe_up ) ;

    g.setColor( old_color ) ;
  }

  static void drawIfPresent(Graphics g, Tonality t, int x, int y, 
			    Note n, Note bottom, Note top, int hampe) {
    if( n!= null ) {
      int diff_y = Intervalle.positionDifference( bottom, n ) ;
      n.setXY( x, y-diff_y*Score.inter_ligne/2) ;
      n.draw(g, t, bottom, top, hampe ) ;
    }
  }

  public Tonality tonality( Tonality t ) {
    try {
      return t.alteredTonality( this ) ;
    }
    catch( Intervalle.TooComplexException e ){
      Harmony.fail( "Chord/tonality: "+t.nom()+" ("+e+")" ) ;
      return null ;
    }
  }

  // Harmony Check methods.

  public void harmonyCheck( Tonality t, Options opts ) 
    throws CheckException 
  {
    checkComplete( voices ) ;
    isWellFormed( opts ) ;
    noCrossing( opts ) ;
    checkRanges( opts ) ;
    chiffrage.check( this, tonality( t ), opts ) ;
    noDoubleLeadingTone( t, opts ) ;
  }

  public void checkComplete( int[] voices ) 
    throws CheckException 
  {
    for( int i = 0 ; i<voices.length ; i++ ) {
      if( notes[voices[i]] == null ) {
	checkFailed( "",
		     "Accord.Complete", 
		     new String[] {TheVoiceName( voices[i] )} ) ;
      }
    }
  }

  private static void checkFailed( String o, String s, Object[] args )
    throws CheckException
  {
    s = Harmony.me.messages.getString( s ) ;
    throw
      new CheckException( o, (new MessageFormat( s )).format( args )) ;
  }


  public void isWellFormed( Options opts ) throws CheckException {
    if( opts.isFalse( "Check.Accord.Formed" )) {
      return ;
    }
    int d0 = notes[0].duree ;

    for( int i = 0 ; i<notes_per_chord-1 ; i++ ) {
      if( notes[i+1].duree != d0 ) {
	checkFailed( "Check.Accord.Formed",
		     "Accord.Formed", 
		     new String[] {TheVoiceName( i ), 
				   theVoiceName( 0 )} ) ;
      }
    }
  }
  
  public void noCrossing( Options opts )
    throws CheckException
  {
    if( opts.isFalse( "Check.Accord.Ascend" )) {
      return ;
    }
    for( int i = 0 ; i<notes_per_chord-1 ; i++ ) {
      if( !Intervalle.isAscendant( notes[i], notes[i+1])) {
	checkFailed( "Check.Accord.Ascend", 
		     "Accord.Ascend", 
		     new String[] {TheVoiceName( i ), 
				   theVoiceName( i+1 )} ) ;
      }
    }
  }

  public void checkRanges( Options opts ) 
    throws CheckException 
  {
    if( opts.isFalse( "Check.Accord.Oct" )) {
      return ;
    }
    for( int i = 0 ; i<notes_per_chord ; i++ ) {
      notes[i].checkRange( "Check.Accord.Oct", range[i], TheVoiceName( i )) ;

      if( i != basse && i != soprano &&
	  Intervalle.isLargerThanOctave( notes[i+1], notes[i] )) {
	checkFailed( "Check.Accord.Oct",
		     "Accord.Oct",
		     new String[] {theVoiceName( i ), 
				   theVoiceName( i+1 )} ) ;
      }
    }
  }

  public void noDoubleLeadingTone( Tonality current_t, Options opts ) 
    throws CheckException 
  {
    if( opts.isFalse( "Check.Accord.DoubleLeading" )) {
      return ;
    }
    Tonality t = tonality( current_t ) ;
    Note sensible = t.noteSensible() ;
    int occur = 0 ;
    for( int i = 0 ; i<notes_per_chord ; i++ ) {
      occur += (Intervalle.isOctave( notes[i], sensible )) ? 1 : 0 ;
    }
    if( occur > 1 ) {
      checkFailed( "Check.Accord.DoubleLeading", 
		   "Accord.DoubleLeading", 
		   new String[] {sensible.nom()} ) ;
    }
  }

  private interface ParallelCheck 
  {
    boolean test( Note n, Note m ) ;
  }

  private static ParallelCheck hasParallelFifths = new ParallelCheck () {
      public boolean test( Note n, Note m ) {
	return Intervalle.isQuinte( n, m ) ;
      }
    } ;
  private static ParallelCheck hasParallelOctaves = new ParallelCheck () {
      public boolean test( Note n, Note m ) {
	return Intervalle.isOctave( n, m ) ;
      }
    } ;

  private static void noParallelWhat( String what, String opt, 
				      ParallelCheck pc ,
				      Chord a, int i, Chord b, int j ) 
    throws CheckException
  {
    if( pc.test( a.notes[i], a.notes[j]) && 
	pc.test( b.notes[i], b.notes[j])) {
      checkFailed( opt, what, new String[] {theVoiceName( i ), 
					    theVoiceName( j )}) ;
    }
  }

  private void noParallelWithVoice( String msg, String opt, Options opts,
				    ParallelCheck pc, int voice, 
				    Chord next, int[] voices )
    throws CheckException
  {
    if( opts.isFalse( opt )) {
      return ;
    }
    for( int j=voice+1 ; j < voices.length ; j++ ) {
      noParallelWhat( msg, opt, pc, this, voices[voice], next, voices[j] ) ;
    }
  }

  public void noParallelism( Chord next, int[] voices, Options opts ) 
    throws CheckException
  {
    for( int i=0 ; i < voices.length-1 ; i++ ) {
      if( notes[voices[i]].hasSamePitch( next.notes[voices[i]] )) {
	continue ;
      }
      noParallelWithVoice( "Accord.ParaFifth", "Check.Accord.ParaFifth", opts,
			   hasParallelFifths, i, next, voices ) ;
      noParallelWithVoice( "Accord.ParaOct", "Check.Accord.ParaOct", opts,
			   hasParallelOctaves, i, next, voices ) ;
    }
  }

  public void noParallelism( Chord next, Options opts ) 
    throws CheckException
  {
    noParallelism( next, voices, opts ) ;
  }

  private void noDirectWhat( String msg, String opt, Options opts,
			     ParallelCheck pc, Chord next )
    throws CheckException
  {
    if( opts.isFalse( opt )) {
      return ;
    }
    if( pc.test( next.notes[basse], next.notes[soprano] )) {
      int s = Intervalle.direction( notes[soprano], next.notes[soprano] ) ;
      int b = Intervalle.direction( notes[basse], next.notes[basse] ) ;

      if( s*b == 1 ) {
	checkFailed( opt, msg, new Object[] {} ) ;
      }
    }
  }

  public void noDirectIntervalles( Chord next, Options opts ) 
    throws CheckException
  {
    noDirectWhat( "Chord.DirectFifth", "Check.Chord.DirectFifth", opts,
		  hasParallelFifths, next ) ;
    noDirectWhat( "Chord.DirectOct", "Check.Chord.DirectOct", opts,
		  hasParallelOctaves, next ) ;
  }

  public void leadingToneToTonic( Tonality current_t, Chord next, 
				  Options opts ) 
    throws CheckException 
  {
    if( opts.isFalse( "Check.Accord.Resolve" )) {
      return ;
    }
    Tonality t = tonality( current_t ) ;
    Note sensible = t.noteSensible() ;
    Note tonique = t.noteTonique() ;
    boolean resolution_possible = false ;

    for( int i = 0 ; i<notes_per_chord ; i++ ) {
      resolution_possible |= 
	Intervalle.isOctave( next.notes[i], tonique ) ;
    }
    if( !resolution_possible ) {
      return ;
    }
    for( int i = 0 ; i<notes_per_chord ; i++ ) {
      if( Intervalle.isOctave( notes[i], sensible ) &&
	  (!Intervalle.isOctave( next.notes[i], tonique ) ||
	   Intervalle.positionDifference( notes[i], next.notes[i] ) != 1 )) {
	checkFailed( "Check.Accord.Resolve",
		     "Accord.Resolve",
		     new String[] {sensible.nom(), t.noteTonique().nom()}) ;
      }
    }
  }

  public boolean isConfigurationChange( Chord next ) {
  next_note:
    for( int i = 0 ; i < notes_per_chord ; i++ ) {
      for( int j = 0 ; j < notes_per_chord ; j++ ) {
	if( notes[i].hasSameName( next.notes[j] )) {
	  continue next_note ;
	}
      }
      return false ;
    }
    return true ;
  }

  public void descendingSeventh( Tonality current_t, Chord next, 
				 Options opts ) 
    throws CheckException 
  {
    if( opts.isFalse( "Check.Chord.DescendingSeventh" ) ||
	!chiffrage.isSeventh()) {
      return ;
    }
    Tonality t = tonality( current_t ) ;
    int seventh_position = 
      chiffrage.positionSeventh( notes[Chord.basse], t ) ;
    Note seventh = t.gamme[seventh_position] ;

    if( Intervalle.isOctave( seventh, t.noteSensible())) {
      return ;
    }
    for( int i = 0 ; i<notes_per_chord ; i++ ) {
      if( Intervalle.isOctave( notes[i], seventh ) &&
	  !Intervalle.isAscendant( next.notes[i], notes[i] )) {
	checkFailed( "Check.Chord.DescendingSeventh",
		     "Chord.DescendingSeventh",
		     new String[] {seventh.nom()}) ;
      }
    }
  }

  public void preparedSeventh( Tonality current_t, Chord next, Options opts ) 
    throws CheckException 
  {
    if( opts.isTrue( "Check.Chord.PreparedSeventh" ) ||
	!next.chiffrage.isSeventh()) {
      return ;
    }
    Tonality t = tonality( current_t ) ;
    int next_seventh_position = 
      next.chiffrage.positionSeventh( next.notes[Chord.basse], t ) ;

    for( int i = 0 ; i<notes_per_chord ; i++ ) {
      if( next.notes[i].position == next_seventh_position &&
	  !next.notes[i].hasSamePitch( notes[i] )) {
	checkFailed( "Check.Chord.PreparedSeventh",
		     "Chord.PreparedSeventh",
		     new String[] {next.notes[i].nom()}) ;
      }
    }
  }

  public void falseRelation( Chord next, Options opts ) 
    throws CheckException
  {
    if( opts.isFalse( "Check.Chord.FalseRelation" )) {
      return ;
    }
    boolean[] is_altered = new boolean[] {false, false, false, false} ;
    boolean[] is_false_relation = (boolean[])is_altered.clone() ;

    for( int i = 0 ; i < notes_per_chord ; i++ ) {
      is_altered[i] = notes[i].isAltered( next.notes[i] ) ;
      
      for( int j = 0 ; j < notes_per_chord ; j++ ) {
	is_false_relation[i] |= 
	  notes[i].isAltered( next.notes[j] ) && i != j ;
      }
    }
    for( int i = 0 ; i < notes_per_chord ; i++ ) {
      if( is_false_relation[i] && !is_altered[i] ) {
	checkFailed( "Check.Chord.FalseRelation",
		     "Chord.FalseRelation",
		     new String[] {TheVoiceName( i )} ) ;
      }
    }
  }

  public void notAugmented( String opt, Options opts,
			    Chord next, int diff, String name )
    throws CheckException 
  {
    if( opts.isFalse( opt )) {
      return ;
    }
    for( int i = 0 ; i<notes_per_chord ; i++ ) {
      try {
	if( (diff == Intervalle.getName( notes[i], next.notes[i] )) &&
	    Intervalle.isAugmented( notes[i], next.notes[i] )) {
	  String jump = new Intervalle( notes[i], next.notes[i] ).nom() ;
	  checkFailed( opt,
		       "Accord.NoAugmented",
		       new String[] {jump, theVoiceName( i )}) ;
	}
      }
      catch( Intervalle.TooComplexException e ) {
      }
    }
  }

  // Counterpoint methods

  // Allowed chiffrages, from least to most specific.

  public Chiffrage counterpointCheck( int[] voices, Tonality t ) 
    throws CheckException
  {
    Chiffrage[] allowed_chiffrages = {
      new Chiffrage( Chiffrage.octave ),
      new Chiffrage( Chiffrage.tierce ),
      new Chiffrage( Chiffrage.quinte ),
      new Chiffrage( Chiffrage.sixte )
	} ;
    int[][] chiffrages_prohibited_by = {
      {},
      {},
      {Chiffrage.sixte},
      {Chiffrage.quinte}
    } ;
    BitSet prohibited_chiffrages = 
      new BitSet( Chiffrage.chiffres_connus.length ) ;
    Note bass = notes[voices[0]] ;
    Chiffrage chiffrage = null ;

    checkComplete( voices ) ;

  voices:
    for( int i = 0 ; i<voices.length ; i++ ) {
      Note n = notes[voices[i]] ;
      n.checkRange( "Check.Accord.Oct", 
		    range[voices[i]], TheVoiceName( voices[i] )) ;
      
      for( int j = 0 ; j<allowed_chiffrages.length ; j++ ) {
	if( allowed_chiffrages[j].allows( n, bass, t )) {
	  for( int k = 0 ; k<chiffrages_prohibited_by[j].length ; k++ ) {
	    prohibited_chiffrages.set( chiffrages_prohibited_by[j][k] ) ;
	  }
	  if( prohibited_chiffrages.get( allowed_chiffrages[j].index )) {
	    checkFailed( "",
			 "Chord.IncompatibleNotes",
			 new Object[] {} ) ;
	  }
	  chiffrage = allowed_chiffrages[j] ;
	  continue voices ;
	}
      }
      String s = 
	Harmony.me.messages.getString( "Chord.OnlyFifthsOrSixths" ) ;
      throw new CheckException( "", TheVoiceName( voices[i] )+" "+s ) ;
    }
    return chiffrage ;
  }

  // Advise methods.

  public void advise( Sequence s, Options opts ) 
    throws AdviseException 
  {
    try {
      harmonyCheck( s.tonality, opts ) ;
    }
    catch( CheckException e ) {
      throw new AdviseException( e.getMessage()) ;
    }
    Note tonique = s.tonality.noteTonique() ;
    Note b = notes[basse] ;

    if( chiffrage.isQuinte()) {
      adviseDouble( degre.equals( Degre.sixtieme ),
		    "Accord.Tonic", tonique, "Accord.Bass", b ) ;
    }
    else if( chiffrage.isSixte()) {
      if( Intervalle.isOctave( b, tonique) || 
	  Intervalle.isQuinte( b, tonique)) {
	adviseDouble( true, "Accord.Bass", b, "", b) ;
      }
      if( degre.equals( Degre.quatrieme )) {
	adviseDouble( true, "Accord.Tonic", tonique, "", b) ;
      }
      if( degre.equals( Degre.deuxieme )) {
	adviseDouble( true, 
		      "Accord.Subdominant", s.tonality.noteSubdominant(), 
		      "", b) ;
      }
      if( notes[soprano].position != chiffrage.positionSixte()) {
	String ss = "Accord.SixthSoprano" ;
	throw new AdviseException( Harmony.me.messages.getString( ss )) ;
      }
      adviseDouble( true, "Accord.Sixth", notes[ soprano ], "", b ) ;
    }
  }

  private void adviseDouble( boolean which,
			     String yes_name, Note yes_note,
			     String no_name, Note no_note )
    throws AdviseException 
  {
    Note doublee = (which) ? yes_note : no_note ;
    String what = (which) ? yes_name : no_name ;
    int occur = 0 ;

    for( int i = 0 ; i<notes_per_chord ; i++ ) {
      occur += (Intervalle.isOctave( notes[i], doublee ))? 1 : 0 ;
    }
    if( occur <= 1 ) {
      String s = Harmony.me.messages.getString( "Accord.AdviseDouble" ) ;
      MessageFormat mf = new MessageFormat( s ) ;
      String ms = 
	mf.format( new String[] {degre.nom(), 
				 Harmony.me.messages.getString( what ), 
				 Note.noms[ doublee.position ]}) ;
      throw new AdviseException( ms ) ;
    }
  }

  //

  static String VoiceName( int i ) {
    return Harmony.me.messages.getString( Voice_Names[i] ) ;
  }

  static String theVoiceName( int i ) {
    return Harmony.me.messages.getString( "Accord.theVoiceName"+i ) ;
  }

  static String TheVoiceName( int i ) {
    return Harmony.me.messages.getString( "Accord.TheVoiceName"+i ) ;
  }
}
    
