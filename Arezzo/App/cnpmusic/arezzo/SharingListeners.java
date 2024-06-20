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
import com.sun.media.jsdt.* ;
import java.awt.* ;
import java.awt.event.* ;
import java.util.* ;
import java.applet.* ;

public class SharingListeners
  extends HarmonyListeners 
  implements ChannelConsumer, Constants 
{
  Channel channel ;
  com.sun.media.jsdt.Client client ;
  Session session ;

  SharingListeners( User u, 
		    String server, 
		    String channel_name, boolean is_new ) 
    throws Exception
  {
    URLString url = 
      URLString.createSessionURL( server, 
				  sharing_port, sharing_session_type, 
				  sharing_session_name ) ;
    client = new SharingClient( u.getName()) ;
    session = SessionFactory.createSession( client, url, false ) ;

    if( is_new && session.channelExists( channel_name )) {
      throw new Exception(  "Channel "+channel_name+" already exists !" );
    }
    session.join( client ) ;
    channel = session.createChannel( client, channel_name, 
				     true, true, true ) ;
    channel.addConsumer( client, this ) ;
  }

  public void disconnect() 
    throws Exception
  {
    channel.removeConsumer( client, this ) ;
    channel.leave( client ) ;

    if( participants().length == 0 ) {
      channel.destroy( client ) ;
    }
    session.leave( client ) ;
  }

  public String[] participants()
    throws Exception
  {
    return channel.listConsumerNames() ;
  }

  // ChannelConsumer

  public synchronized void dataReceived( Data d ) {
    processEvents( d.getDataAsString()) ;
    Harmony.me.repaint() ;
  }

  public void processEvents( String commands) {
    LocalListeners ll = Harmony.me.local_listeners ;
    StringTokenizer st = new StringTokenizer( commands, " \t\n" ) ;

    while( st.hasMoreElements()) {
      String command = st.nextToken() ;
      
      if( debug_SharingListeners ) {
	System.err.println( "SharingListeners/processEvents:"+command+"." ) ;
      }
      //
      // UI events
      //
      if( command.equals( "mousePressed" )) {
	int x = Integer.parseInt( st.nextToken()) ;
	int y = Integer.parseInt( st.nextToken()) ;
	int ml = Integer.parseInt( st.nextToken()) ;
	HarmonyListeners.is_mouse_left = (ml == MOUSE_LEFT) ;
	ll.processMousePressed( x, y ) ;
      }
      else if( command.equals( "keyPressed" )) {
	ll.processKeyPressed( Integer.parseInt( st.nextToken())) ;
      }
      else if( command.equals( "mouseDragged" )) {
	int x = Integer.parseInt( st.nextToken()) ;
	int y = Integer.parseInt( st.nextToken()) ;
	ll.processMouseDragged( x, y ) ;
      }
      else if( command.equals( "adjustmentValueChanged" )) {
	String s = st.nextToken() ;
	try {
	  float f = (new Float( s )).floatValue() ;
	  ll.processAdjustmentValueChanged( f ) ;
	}
	catch( NumberFormatException e ) {
	  Harmony.fail( "Float expected, got "+s ) ;
	}
      }
      //
      // Non-UI events
      //
      else if( command.equals( "synchronize" )) {
	Harmony.me.score = 
	  new Score( Harmony.me.score.name, 
		     new Sequence( commands.substring( command.length()+1 ))) ;
      }
      //
      // Menu events
      //
      else {
	String ac = command+" "+st.nextToken() ;
	ll.get( ac ).actionPerformed( null ) ;
      }
    }
  }

  // MouseListener

  private void writeLine( String line ) {
    try {
      Data data = new Data( line ) ;
      data.setPriority( Channel.HIGH_PRIORITY ) ;
      channel.sendToAll( client, data ) ;
    }
    catch( Exception e ) {
      System.err.print("writeLine/when sending:"+line+":" ) ;
      e.printStackTrace() ;
    }
  }
    
  static final int MOUSE_LEFT = 1 ;
  static final int MOUSE_RIGHT = 0 ;

  public void mousePressed( MouseEvent e ) {
    boolean is_mouse_left = 
      ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0);
    writeLine( "mousePressed"+" "+e.getX()+" "+e.getY()+" "+
	       (is_mouse_left ? MOUSE_LEFT : MOUSE_RIGHT)+"\n" ) ;
  }

  boolean mouse_dragging = false ;
  MouseEvent last_dragged_event ;
    
  private void processDragging() {
    if( mouse_dragging ) {
      mouse_dragging = false ;
      writeLine( "mouseDragged"+" "+
		 last_dragged_event.getX()+" "+
		 last_dragged_event.getY()+"\n" ) ;
    }
  }

  public void mouseReleased( MouseEvent e ) {
    processDragging() ;
  }

  // KeyListener

  public void keyPressed( KeyEvent e ) {
    writeLine( "keyPressed"+" "+e.getKeyCode()+"\n" ) ;
  }

  // MouseMotionListner

  public void mouseDragged( MouseEvent e ) {
    mouse_dragging = true ;
    last_dragged_event = e ;
    Harmony.me.local_listeners.processMouseDragged( e.getX(), e.getY()) ;
  }

  // FocusListener

  // ActionListener

  public void actionPerformed( ActionEvent e ) {
    String command = e.getActionCommand() ;

    if( debug_SharingListeners ) {
	System.err.println( "SharingListeners/actionPerformed:"+command+":" ) ;
    }
    writeLine( command ) ;
  }

  // AdjustementListener

  public void adjustmentValueChanged( AdjustmentEvent e ) {
    Scrollbar sc = Harmony.me.scroll ;

    writeLine( "adjustmentValueChanged"+" "+
	       (float)sc.getValue()/sc.getMaximum()) ;
  }

  // Misc. operations

  public void synchronize() {
    Sequence s = Harmony.me.score.sequence ;
    writeLine( "synchronize"+" "+s.sprintf()) ;
  }
}

