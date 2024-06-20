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
import java.util.* ;
import java.text.* ;

// A sequence is a Vector of Chords
//
public class Sequence 
  extends Vector 
  implements Serializable 
{
  Tonality tonality ;
  Mesure mesure ;
  Options check_options = default_check_options ;
  String comments = "" ;

  static Options default_check_options ;

  static int chord_start_x ; // apres clef et tonalite

  private int y_bar_count_offset = 50 ;

  static {
    default_check_options = 
      new Options( new String[] {"Check.Accord.Formed",
				 "Check.Accord.Ascend",
				 "Check.Accord.Oct",
				 "Check.Accord.DoubleLeading",
				 "Check.Accord.ParaFifth",
				 "Check.Accord.ParaOct",
				 "Check.Accord.Resolve",
				 "Check.Chord.DescendingSeventh",
				 "Check.Chord.PreparedSeventh",
				 "Check.Chord.FalseRelation",
				 "Check.Chord.DirectFifth",
				 "Check.Chord.DirectOct",
				 "Check.NoAugmentedSecond",
				 "Check.NoAugmentedFourth",
				 "Check.Chiffrage.Missing",
				 "Check.Chiffrage.Belong",
				 "Check.Voice.Foreign",
				 "Check.Voice.Second",
				 "Check.Voice.Melodic",
				 "Check.Voice.Intervalle",
				 "Check.Voice.Direction",
				 "Check.Voice.Third",
				 "Check.Voice.NotCompleted",
				 "Check.Voice.Balance"},
		   Options.option_true ) ;
  }

  public Sequence() {
    super() ;
    try {
      tonality = new Tonality() ;
    }
    catch( Intervalle.TooComplexException e ) {
    }
    mesure = new Mesure() ;
    Chord c = new Chord() ;
    c.setDegre( tonality ) ;
    addElement( c ) ;
  }
  
  Sequence( String s ) {
    super() ;

    try {
      String version = new StringTokenizer( s ).nextToken() ;
      s = s.substring( version.length()+1 ) ;

      if( version.equals( Constants.version_2_1 )) {
	Cypher d = new Cypher( Constants.cypher_key ) ;
	s = d.go( s, false ) ;
      }
      if( version.equals( Constants.version_2_0 ) ||
	  version.equals( Constants.version_2_1 )) {
	StringTokenizer st = new StringTokenizer( s ) ;

	try {
	  tonality = new Tonality( st ) ;
	}
	catch( Intervalle.TooComplexException e ) {
	  Harmony.fail( "Sequence - Malformed data file : "+s ) ;
	}
	mesure = new Mesure( st ) ;
	
	while( st.hasMoreElements()) {
	  this.addElement(new Chord( st, tonality )) ;
	}
      }
    }
    catch( IOException e ) {
      Harmony.fail( "Sequence - Malformed data file : "+s ) ;
    }
  }

  Sequence( StringTokenizer st ) 
    throws Exception
  {
    super() ;
    String version = st.nextToken() ;

    if( !version.equals( Constants.version_2_0 )) {
      throw new Exception( "Only 2.0 is supported: got "+version ) ;
    }
    tonality = new Tonality( st ) ;
    mesure = new Mesure( st ) ;
	
    while( st.hasMoreElements()) {
      this.addElement(new Chord( st, tonality )) ;
    }
  }

  String sprintf() {
    return sprintf( Constants.version_2_0 ) ;
  }

  String sprintf( String v ) {
    String s = tonality.sprintf()+"\n"+mesure.sprintf()+"\n" ;

    for( Enumeration e = this.elements() ; e.hasMoreElements() ; ) {
	s += ((Chord)e.nextElement()).sprintf() ;
    }
    if( v.equals( Constants.version_2_1 )) {
      Cypher c = new Cypher( Constants.cypher_key ) ;
      s = c.go( s, true ) ;
    }
    return v+"\n"+s ;
  }

  public int selectedChordNumber( int x, int y, int i0 ) {
    int i = (x-chord_start_x)/Score.inter_chord+i0 ;

    if( i < 0 ) {
      Harmony.fail("SelectedChordNumber/Incorrect number: "+i ) ;
    }
    return  Math.min( i, size()-1 ) ;
  }
    
  public Chord insertAfter( Chord a ) {
    int idx = indexOf( (Object)a ) ;

    if( idx != -1 ) {
      Chord n = new Chord() ;
      insertElementAt( n, idx+1 ) ;
      return n ;
    }
    return null ;
  }

  public void removeChord( Chord a ) {
    int idx = indexOf( (Object)a ) ;
    removeElementAt( idx ) ;
    Harmony.me.score.selected_chord = null ;
  }

  // MThd (ASCII)
  // longueur de chunk
  // format 0
  // 1 piste
  // 480 ticks a la noire 

  static byte[] header_chunk = {
    (byte)0x4d, (byte)0x54, (byte)0x68, (byte)0x64,	
    0, 0, 0, (byte)0x06,		
    0, 0,			
    0, (byte)0x01,			
    (byte)0x01, (byte)224
  };
  static int noire_ticks = header_chunk[ 12 ]<<8+header_chunk[ 13 ] ;

  // MTrk (ASCII)

  static byte[] music_track_begin = {
    (byte)0x4d, (byte)0x54, (byte)0x72, (byte)0x6b
  };

  // Duree - longueur de chunk (a modifier)

  static byte[] music_track_time = {
    0x0, 0x0, 0x0, 0x0
  };

  // Control Change Bank Select LSB (Cubase)
  // Control Change Bank Select MSB (Cubase)
  // Program Change GM Piano 
  // (at program_change_index+bytes_between_PC*[basse,..,soprano])
  // Set Tempo meta-event type

  static byte[] music_track_select = {
    0, (byte)0xb0, (byte)0x20, (byte)0x09,
    0, (byte)0xb0, 0, 0,
    0, (byte)0xc0, Chord.midi_GMpiano,
    0, (byte)0xc1, Chord.midi_GMpiano,
    0, (byte)0xc2, Chord.midi_GMpiano,
    0, (byte)0xc3, Chord.midi_GMpiano,
    0, (byte)0xff, (byte)0x51, (byte)0x03
  };
  static int program_change_index = 10 ;
  static int bytes_between_PC = 3 ;

  // Tempo (en microsecondes par noire)
  // Time signature (mesure) (Cubase)

  static byte[] music_track_tempo = {  
       (byte)0x10, (byte)0x84, (byte)0x18,
    0, (byte)0xff, (byte)0x58, (byte) 0x04,
    (byte)0x04, (byte)0x02, (byte)0x18, (byte)0x08
  };

  static byte[] end_event = {
      0, (byte)0xff, (byte)0x2f, 0
  };
  static int bytes_par_note = 4 ;

  public void saveMIDI( DataOutputStream st ) 
    throws IOException 
  {
      st.write( header_chunk, 0, header_chunk.length ) ;
      int len =			// (4 Notes on + delta + 4 Notes off)*size
				// + music_track_end + fin
	(2*Chord.notes_per_chord*bytes_par_note+1)*this.size()+
	music_track_select.length+music_track_tempo.length+
	end_event.length ;

      st.write( music_track_begin, 0, music_track_begin.length ) ;
      for( int i = music_track_time.length-1 ; i >= 0 ; i -- ) {
	  music_track_time[i] = (byte)(len & 0xff) ;
	  len >>>= 8 ;
      }
      st.write( music_track_time, 0, music_track_time.length ) ;
      st.write( music_track_select, 0, music_track_select.length ) ;
      st.write( music_track_tempo, 0, music_track_tempo.length ) ;

      for( Enumeration e = this.elements() ; e.hasMoreElements() ; ) {
	  Chord a = (Chord)e.nextElement() ;
	  a.saveMIDI( st ) ;
      }
      st.write( end_event, 0, end_event.length ) ;
      st.flush() ;
  }

  public static void updateTempo( int t ) {
    t *= 1000 ; // Convert to microseconds
    music_track_tempo[2] = (byte)(t&0xff) ;
    t >>>= 8 ;
    music_track_tempo[1] = (byte)(t&0xff) ;
    t >>>= 8 ;
    music_track_tempo[0] = (byte)(t&0xff) ;
  }

  public static void updateInstrument( int voice, int instrument ) {
    music_track_select[program_change_index+bytes_between_PC*voice] = 
      (byte)instrument ;
  }

  //

  String abc() {
    Mesure m = (Mesure)mesure.clone() ;
    String s= "" ;
    
    for( int i = 0 ; i<Chord.notes_per_chord ; i++ ) {
      Note c = (i <= Chord.tenor ) ? Note.do2 : Note.do4 ;
      s += "V:"+i+((i <= Chord.tenor) ? " clef=f\n" : "\n") ;
      s += new Voice( this, i ).abc( m, c )+"\n" ;
      m.resetCounter() ;
    }
    return s ;
  }

  // x,y est en bas a gauche du premier accord.

  public void draw(Graphics g, int i0, int x, int y ) {
    tonality.draw(g, x, y) ;
    x += tonality.width() ;
    mesure.draw( g, x, y-Score.y_offset_mesure ) ;
    x += mesure.width() ;
    chord_start_x = x ;    
    
    mesure.resetCounter() ;

    for( int i = 0 ; i<i0 ; i++ ) {
      mesure.addCounter( ((Chord)elementAt(i)).duree()) ;
    }
    for( int i = i0 ; i<size() ; i++ ) {
      Chord a = (Chord)elementAt(i) ;
      a.draw( g, tonality, x, y ) ;

      mesure.addCounter( a.duree()) ;

      if( mesure.isCompleted()) {
	int x_mid = x+Score.inter_chord/2 ;
	int h = 
	  (Score.nombre_lignes-1)*Score.inter_ligne+
	  Score.diff_y_bas_y_haut ;
	g.drawLine( x_mid, y, x_mid, y-h ) ;

	if( i == size()-1 ) {
	  g.drawLine( x_mid+Score.inter_barre, y, 
		      x_mid+Score.inter_barre, y-h ) ;
	}
	else {
	  if( Score.display_options.isTrue( "Score.ShowBarNumber" )) {
	    g.drawString( "("+(mesure.barCount()+1)+")",
			  x_mid, y-h-y_bar_count_offset ) ;
	  }
	}
      }
      x += Score.inter_chord ;
    }
  }

  public int width() {
    return tonality.width()+size()*Score.inter_chord ;
  }

  public void setDegres() {
    for( Enumeration e = this.elements() ; e.hasMoreElements() ; ) {
	Chord a = (Chord)e.nextElement() ;
	a.setDegre( tonality ) ;
    }
  }
    
  // The brain ... 

  public void harmonyCheck() 
    throws CheckException 
  {
    int i = -1 ;

    try {
      Chord current, prev = null ;
      
      for( i = 0 ; i<this.size() ; prev = current, i++ ) {
	current = (Chord)this.elementAt(i) ;
	current.harmonyCheck( tonality, check_options ) ;

	if( prev != null ) {
	  prev.noParallelism( current, check_options ) ;

	  if( prev.isConfigurationChange( current )) {
	    continue ;
	  }
	  prev.leadingToneToTonic( tonality, current, check_options ) ;
	  prev.descendingSeventh( tonality, current, check_options ) ;
	  prev.preparedSeventh( tonality, current, check_options ) ;
	  prev.falseRelation( current, check_options ) ;
	  prev.noDirectIntervalles( current, check_options ) ;

	  String snd = 
	    Harmony.me.messages.getString( "Sequence.Second" ) ;
	  prev.notAugmented( "Check.NoAugmentedSecond", check_options,
			     current, Intervalle.second, snd ) ;
	  String fth = 
	    Harmony.me.messages.getString( "Sequence.Fourth" ) ;
	  prev.notAugmented( "Check.NoAugmentedFourth", check_options,
			     current, Intervalle.fourth, fth ) ;
	}
      }    
    }
    catch(CheckException e) {
      throwCheckExceptionInChord( i, e ) ;
    }
  }

  public void melodyCheck( int voice ) 
    throws CheckException 
  {
    try {
      (new Voice( this, voice )).check( tonality, mesure, check_options ) ;
    }
    catch( CheckException e ) {
      String s = Harmony.me.messages.getString( "Sequence.MelodyCheck" ) ;
      MessageFormat mf = new MessageFormat( s ) ;
      throw new CheckException( e.name,
				mf.format( new String[] {
				  e.getMessage(), 
				  Chord.VoiceName( voice )
				})) ;
    }
  }

  public int max_consecutive_intervalles = 3 ;

  public void counterpointCheck( int[] voices ) 
    throws CheckException
  {
    for( int i = 0 ; i<voices.length ; i++ ) {
      melodyCheck( voices[i] ) ;
    }
    Chord current = null, previous = null ;
    int i = 0 ;

    Chiffrage current_ch, previous_ch = null ;
    int ch_count = 0 ;

    mesure.resetCounter() ;

    try {
      for( ; i<size() ; previous = current, previous_ch = current_ch, i++ ) {
	current = (Chord)elementAt(i) ;

	mesure.addCounter( current.duree()) ;

	if( !mesure.isCompleted()) {
	  String s = 
	    Harmony.me.messages.getString( "Sequence.FirstSpecies" ) ;
	  throw new CheckException( "", s ) ;
	}
	current_ch = current.counterpointCheck( voices, tonality ) ;

	if( previous != null ) {
	  previous.noParallelism( current, voices, check_options ) ;
	  ch_count = (current_ch.equals( previous_ch )) ? ch_count+1 : 1 ;
	}
	else {
	  ch_count = 1 ;
	}
	if( ch_count > max_consecutive_intervalles ) {
	  String s = 
	    Harmony.me.messages.getString( "Sequence.SuccessiveChiffrages" ) ;
	  MessageFormat mf = new MessageFormat( s ) ;
	  throw new CheckException( "",
				    mf.format( new String[] {
				      ""+max_consecutive_intervalles
				    })) ;
	}
      }
    }
    catch(CheckException e) {
      throwCheckExceptionInChord( i, e ) ;
    }
  }

  private int relativeChordNumber( int i ) {
    int chord_nb = 0 ;

    mesure.resetCounter() ;

    for( int j=0 ; j < i ; j++ ) {
      mesure.addCounter( ((Chord)elementAt( j )).duree()) ;
      chord_nb = (mesure.isCompleted()) ? 0 : chord_nb+1 ;
    }
    return chord_nb ;
  }

  private void throwCheckExceptionInChord( int i, CheckException e ) 
    throws CheckException
  {
    Harmony.me.score.selected_note = null ;
    Harmony.me.score.selected_chord = (Chord)elementAt(i) ;

    String s = Harmony.me.messages.getString( "Sequence.InChord" ) ;
    MessageFormat mf = new MessageFormat( s ) ;
    String ms =
      mf.format( new Object[] {e.getMessage(), 
			       new Integer( relativeChordNumber( i )+1 ),
			       new Integer( mesure.barCount()+1 )} ) ;
    throw new CheckException( e.name, ms ) ;
  }

  //

  public void harmonyAdvise( Chord a ) 
    throws AdviseException 
  {
    if( a != null ) {
      a.advise( this, check_options ) ;
    }
    if( a == null ) {
      String s = Harmony.me.messages.getString( "Sequence.Select" ) ;
      throw new AdviseException( s ) ;
    }
  }

  public void melodyAdvise( int voice ) 
    throws AdviseException 
  {
    Voice v = new Voice( this, voice ) ;

    try {
      v.check( tonality, mesure, check_options ) ;
    }
    catch( CheckException e ) {
      String s = Harmony.me.messages.getString( "Sequence.MelodyCheck" ) ;
      MessageFormat mf = new MessageFormat( s ) ;
      throw new AdviseException( mf.format( new String[] {
	e.getMessage(), 
	Chord.VoiceName( voice )
      })) ;
    }
    v.advise( tonality, mesure ) ;
  }
}
