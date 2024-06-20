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
import java.util.Enumeration ;
import java.text.* ;

public class Voice
implements Enumeration
{
  Sequence sequence ;
  int voice ; 

  private int current_element_count ;

  Voice( Sequence s, int v ) {
    voice = v ;
    sequence = s ;
    resetCounter() ;
  }
  
  public boolean hasMoreElements() {
    return (current_element_count+1 < sequence.size()) ;
  }

  public Object nextElement() {
    if( hasMoreElements()) {
      Chord a = (Chord)sequence.elementAt( ++current_element_count ) ;
      return a.notes[ voice ] ;
    }
    return( null ) ;
  }

  public Object firstElement() {
    current_element_count = 0 ;
    return ((Chord)sequence.firstElement()).notes[ voice ] ;
  }

  public void resetCounter() {
    current_element_count = -1 ;
  }

  //
  
  String abc( Mesure m, Note c ) {
    String s = "" ;
    resetCounter() ;

    do {
      Note n = (Note)nextElement() ; 
      int duree = (n == null) ? currentChord().duree() : n.duree ;
      s += (n == null) ? "z/"+duree : n.abc( c, sequence.tonality ) ;
      m.addCounter( duree ) ;
      
      if( m.isCompleted()) {
	s+="|" ;
      }
    }
    while( hasMoreElements()) ;
    return s+"|" ;
  }

  // Check methods

  private void checkForeign( Note n, Tonality t, Options opts ) 
    throws CheckException
  {
    if( opts.isFalse( "Check.Voice.Foreign")) {
      return ;
      }
    if( n.isForeign( t )) {
      String s = Harmony.me.messages.getString( "Voice.ForeignNote" ) ;
      MessageFormat mf = new MessageFormat( s ) ;
      throw
	new CheckException( "Check.Voice.Foreign",
			    mf.format( new String[] {n.nom(), t.nom()})) ;
    }
  }

  private void checkSecond( Intervalle jump, Tonality t, Options opts ) 
    throws CheckException
  {
    if( opts.isFalse( "Check.Voice.Second")) {
      return ;
    }
    if( jump.isForeignSecond( t )) {
      String s = Harmony.me.messages.getString( "Voice.ForeignIntervalle" ) ;
      MessageFormat mf = new MessageFormat( s ) ;
      throw
	new CheckException( "Check.Voice.Second",
			    mf.format( new String[] {jump.nom(), t.nom()})) ;
    }
  }

  public void check( Tonality t, Mesure m, Options opts ) 
    throws CheckException 
  {
    Intervalle previous_jump = null ;

    //next_direction = 0 if (next jump).direction() doesn't matter, 
    //                 1 must go up, 
    //                 -1 down
    //
    int next_direction = 0 ; 

    m.resetCounter() ;

    // Sum of up and down jumps
    //
    int balance = 0 ;

    Note current = (Note)firstElement() ;
    Note next = (Note)nextElement() ; 

    try {
      for( ; current != null ; next = (Note)nextElement()) {
	m.addCounter( current.duree ) ;

	checkForeign( current, t, opts ) ;

	if( next == null ) {
	  break ;
	}
	Intervalle jump = new Intervalle( current, next ) ;
	int direction = jump.direction() ;

	if( opts.isTrue( "Check.Voice.Melodic") &&
	    !jump.isMelodic()) {
	  String s = Harmony.me.messages.getString( "Voice.NotMelodic" ) ;
	  MessageFormat mf = new MessageFormat( s ) ;
	  throw
	    new CheckException( "Check.Voice.Melodic",
				mf.format( new String[] {jump.nom()})) ;
	}
	if( opts.isTrue( "Check.Voice.Direction") &&
	    next_direction*direction == -1 ) {
	  String where = 
	    Harmony.me.messages.getString( (next_direction == -1) ? 
					   "Voice.Descending" :
					   "Voice.Ascending" ) ;
	  String s = Harmony.me.messages.getString( "Voice.Direction" ) ;
	  MessageFormat mf = new MessageFormat( s ) ;
	  throw
	    new CheckException( "Check.Voice.Direction",
				mf.format( new String[] 
				  {previous_jump.nom(), jump.nom(), where})) ;
	}
	if( opts.isTrue( "Check.Voice.Third") &&
	    jump.isTierce()) {
	  String s = Harmony.me.messages.getString( "Voice.Third" ) ;
	  MessageFormat mf = new MessageFormat( s ) ;
	  String thd = mf.format( new String[] {jump.nom()}) ;

	  Note next_next = (Note)nextElement() ;

	  if( next_next == null ) {
	    throw new CheckException( "Check.Voice.Third", thd ) ;
	  }
	  Intervalle next_jump = new Intervalle( next, next_next ) ;
	  
	  if( !next_jump.isSeconde()) {
	    current_element_count-- ;
	    throw new CheckException( "Check.Voice.Third", thd ) ;
	  }
	  if( direction*next_jump.direction() != -1 ) {
	    current_element_count-- ;
	    throw new CheckException( "Check.Voice.Third", thd ) ;
	  }
	  try {
	    checkSecond( next_jump, t, opts ) ;
	    checkForeign( next, t, opts ) ;
	  }
	  catch( CheckException e ) {
	    current = next ;
	    throw e ;
	  }
	  balance += direction*next_jump.positionDifference() ;
	  m.addCounter( next.duree ) ;

	  // Step forward for thirds

	  next_direction = 0 ;
	  previous_jump = jump ;
	  current = next_next ;
	  continue ;
	}
	if( jump.isRepetition()) {
	}
	else if( jump.isSeconde()) {
	  checkSecond( jump, t, opts ) ;
	  balance += direction*jump.positionDifference();
	}
	else if( jump.isQuarte() || jump.isQuinte() || jump.isSixte()) {
	  balance += direction*jump.positionDifference() ;
	}
	else if( jump.isOctave()) {
	  balance += direction*Intervalle.intervalles_number ;
	}
	else if( opts.isTrue( "Check.Voice.Intervalle")) {
	  String s = Harmony.me.messages.getString( "Voice.Intervalle" ) ;
	  MessageFormat mf = new MessageFormat( s ) ;
	  throw 
	    new CheckException( "Check.Voice.Intervalle",
				mf.format( new String[] {jump.nom()})) ;
	}

	// Standard step forward

	next_direction = 
	  (jump.isRepetition()) ? 0 : (jump.isOctave()) ? -direction : 0 ;
	previous_jump = jump ;
	current = next ;
      }
    }
    catch( CheckException e ) {
      Harmony.me.score.selected_note = current ;
      Harmony.me.score.selected_chord = 
	(current_element_count != 0) ? previousChord() : currentChord() ;

      String s = Harmony.me.messages.getString( "Voice.InChord" ) ;
      MessageFormat mf = new MessageFormat( s ) ;
      String ms = mf.format( new Object[] {
	e.getMessage(), new Integer( current_element_count )
      }) ;
      throw new CheckException( e.name, ms ) ;
    }
    catch( Intervalle.TooComplexException e ) {
      Harmony.me.score.selected_note = current ;
      Harmony.me.score.selected_chord = 
	(current_element_count != 0) ? previousChord() : currentChord() ;

      throw
	new CheckException( "",
			    Harmony.me.messages.getString( "Voice.TooComplex" )) ;
    }
    if( opts.isTrue( "Check.Voice.NotCompleted") &&
	!m.isCompleted()) {
      String s = Harmony.me.messages.getString( "Voice.NotCompleted" ) ;
      throw new CheckException ( "Check.Voice.NotCompleted", s ) ;
    }
    if( opts.isTrue( "Check.Voice.Balance") &&
	balance != 0 ) {
      String sb = Harmony.me.messages.getString( "Voice.Balance" ) ;
      MessageFormat mf = new MessageFormat( sb ) ;
      String sf = 
	mf.format( new Object[] 
	  {new Integer( Math.abs( balance )),
	   ((balance > 0) ? 
	    Harmony.me.messages.getString( "Voice.Descending" ) :
	    Harmony.me.messages.getString( "Voice.Ascending" ))}) ;
      throw new CheckException( "Check.Voice.Balance", sf ) ;
    }
  }

  // Advise methods

  static double seconds_per_centage_minimum = 0.5 ;

  public void advise( Tonality t, Mesure m ) 
    throws AdviseException 
  {
    Note current = (Note)firstElement() ;
    Note next = (Note)nextElement() ; 

    int total_jumps = 0 ;
    int second_jumps = 0 ;

    for( ; next != null ; next = (Note)nextElement()) {
      try {
	Intervalle jump = new Intervalle( current, next ) ;

	if( jump.isTierce()) {
	  next = (Note)nextElement() ;
	  jump = new Intervalle( current, next ) ;
	}
	second_jumps += (jump.isSeconde()) ? 1 : 0 ;
	total_jumps++ ;
	current = next ;
      }
      catch( Intervalle.TooComplexException e ) {
	String s = Harmony.me.messages.getString( "Voice.TooComplex" ) ;
	throw new AdviseException( s ) ;
      }
    }
    if( second_jumps <= seconds_per_centage_minimum*total_jumps ) {
      String s = Harmony.me.messages.getString( "Voice.SecondsAdvise" ) ;
      MessageFormat mf = new MessageFormat( s ) ;
      String args[] = new String[] {
	  Chord.theVoiceName( voice ), 
	  String.valueOf( second_jumps ), 
	  String.valueOf( total_jumps )
      };
      String msg = mf.format( args ) ;
      throw new AdviseException( msg ) ;
    }
  }

  //
  
  private Chord currentChord() {
    return (Chord)sequence.elementAt( current_element_count ) ;
  }

  private Chord previousChord() {
    return (Chord)sequence.elementAt( current_element_count-1 ) ;
  }
}
