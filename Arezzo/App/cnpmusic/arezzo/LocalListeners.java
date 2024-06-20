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
import java.awt.event.* ;
import java.applet.* ;
import java.util.* ;

class LocalListeners
  extends HarmonyListeners 
  implements Constants
{

  Hashtable menu_items ;

  LocalListeners() {
    menu_items = new Hashtable() ;
  }

  // MouseListener

  public void mousePressed( MouseEvent e ) {
    int x = e.getX() ;
    int y = e.getY() ;
    
    // Adding Ctrl-down for right-button challenged Macs 

    is_mouse_left = 
      ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 &&
       !e.isControlDown());

    e.consume() ;
    processMousePressed( x, y ) ;
  }

  private Chord processInsert( Chord a, Score p, int mode, int y ) {
    if( mode == Score.insert_chord_bass ) {
      Chord na = p.sequence.insertAfter( a ) ;
      na.setDuree( a.duree()) ;
      na.notes[Chord.basse].move( y ) ;
      return na ;
    }
    else if( mode == Score.insert_chord_full ) {
      for( Chord na = processInsert( a, p, Score.insert_chord_bass, y ) ;
	   na.insertLowerNote() != null ;
	   ) {
      }
    }
    else if( mode == Score.insert_lowest ) {
      Note n = a.insertLowerNote() ;
	
      if( n != null ) {
	n.move( y ) ;
	p.selected_note = n ;
      }
    }
    else { // Voice inserts
      if( a.notes[mode] == null ) {
	p.selected_note = a.addNoteInVoice( mode ) ;
	p.selected_note.move( y ) ;
      }
    }
    return null ;
  }

  public void processMousePressed( int x, int y ) {
    Score p = Harmony.me.score ;

    if( !p.isSelected( x, y )) {
      return ;
    }
    p.selected_note = null ;
    p.selected_chiffrage = null ;
    p.selected_degre = null ;

    if( mouseLeft()) {
      p.selected_chord = null ;

      if( p.selectedNote( x, y ) != null ||
	  p.selectedChiffrage( x, y ) != null || 
	  p.selectedDegre( x, y ) != null ) {
      }
    }
    else if( mouseRight()) {
      Chord a = p.selectedChord( x, y ) ;

      if( a == null ) {
	return ;
      }
      processInsert( a, p, p.insert_mode, y ) ;
    }
    Harmony.me.repaint() ;
  }

  // KeyListener

  private void processDelete() {
    Score p = Harmony.me.score ;

    if( (p.selected_note != null) ?
	p.selected_chord.removeNote( p.selected_note ) :
	p.selected_chord != null ) {
      p.sequence.removeChord( p.selected_chord ) ;
	
      if( p.sequence.isEmpty()) {
	Harmony.me.score = 
	  new Score( Harmony.score_name_default, new Sequence()) ;
      }
    }
  }

  private void processHorizontalArrow( int limit, int step, int def ) {
    Score p = Harmony.me.score ;

    p.selected_chiffrage = null ;

    if( p.selected_chord != null ) {
      int i = p.sequence.indexOf( (Object)p.selected_chord ) ;
      
      if( i != limit ) {
	Chord new_chord = (Chord)p.sequence.elementAt( i+step ) ;
	
	if( p.selected_note != null ) {
	  p.selected_note = 
	    new_chord.notes[p.selected_chord.voiceIndex( p.selected_note )] ;
	}
	p.selected_chord = new_chord ;
      }
      return ;
    }
    p.selected_chord = (Chord)p.sequence.elementAt( def ) ;
  }

  private void processVerticalArrow( int limit, int step, int def ) {
    Score p = Harmony.me.score ;

    if( p.selected_chord == null ) {
      return ;
    }
    p.selected_chiffrage = null ;
    int i = (p.selected_note == null) ? 
      def :
      p.selected_chord.voiceIndex( p.selected_note )+step ;
      
    for( ; i != limit+step && p.selected_chord.notes[i] == null ; i += step )
      ;
    if( i != limit+step && p.selected_chord.notes[i] != null ) {
      p.selected_note =  p.selected_chord.notes[i] ;
    }
  }

  public void processKeyPressed( int code ) {
    if( code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE ) {
      processDelete() ;
    } 
    else if( code == KeyEvent.VK_LEFT ) {
      processHorizontalArrow( 0, -1, Harmony.me.score.sequence.size()-1 ) ;
    }
    else if( code == KeyEvent.VK_RIGHT ) {
      processHorizontalArrow( Harmony.me.score.sequence.size()-1, 1, 0 ) ;
    }
    else if( code == KeyEvent.VK_UP ) {
      processVerticalArrow( Chord.notes_per_chord-1, 1, Chord.basse ) ;
    }
    else if( code == KeyEvent.VK_DOWN ) {
      processVerticalArrow( 0, -1, Chord.soprano ) ;
    }
    Harmony.me.repaint() ;
  }

  public void keyPressed( KeyEvent e ) {
    int code = e.getKeyCode() ;
    e.consume() ;
    processKeyPressed( code ) ;
  }

  // MouseMotionListner

  public void mouseDragged( MouseEvent e ) {
    int x = e.getX() ;
    int y = e.getY() ;
    e.consume() ;
    processMouseDragged( x, y ) ;
  }

  public void processMouseDragged( int x, int y ) {
    Score p = Harmony.me.score ;

    if( !p.isSelected( x, y )) {
      return ;
    }
    if( mouseLeft()) {
      if( p.selected_note != null ) {
	p.selected_note.move( y ) ;
	p.selected_chord.setDegre( p.sequence.tonality,
				   p.selected_chord.degre.alteration ) ;
      }
    }
    Harmony.me.repaint() ;
  }

  // FocusListener

  public void processFocusGained() {
    Harmony.me.repaint() ;
  }

  public void focusGained( FocusEvent e ) {
    processFocusGained() ;
  }

  // ActionListener for menu items

  public void put( String cmd, ActionListener al ) {
    if( debug_LocalListeners ) {
      System.err.println( "LocalListeners/put: "+cmd+","+al ) ;
    }
    menu_items.put( cmd, al ) ;
  }

  public ActionListener get( String cmd ) {
    return (ActionListener)menu_items.get( cmd ) ;
  }

  public void actionPerformed( ActionEvent e ) {
    String command = e.getActionCommand() ;
    ActionListener al = get( command ) ;

    if( debug_LocalListeners ) {
      System.err.println( "LocalListeners/actionPerformed:"+command ) ;
      System.err.println( "LocalListeners/Event:"+e ) ;
    }
    if( al != null ) {
      al.actionPerformed( e ) ;
    }
    else {
      Harmony.fail( "Unknown command: "+command ) ;
    }
  }

  // Adjustment Listener

  public void adjustmentValueChanged( AdjustmentEvent e ) {
    Scrollbar sc = Harmony.me.scroll ;
    processAdjustmentValueChanged( (float)sc.getValue()/sc.getMaximum()) ;
  }

  public void processAdjustmentValueChanged( float f ) {
    Harmony.me.score.scroll_ratio = f ;
    Harmony.me.repaint() ;
  }
}
