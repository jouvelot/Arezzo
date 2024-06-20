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
import java.io.*;
import java.util.* ;
import java.net.* ;
import java.text.* ;
import java.lang.* ;

public class Arezzo 
  extends Applet 
{
  // Harmony frame

  Frame harmony_frame ;

  static int width = 700 ;
  static int height = 650 ;

  private Image start_image ;
  private Image start_image_red ;
  private Image start_image_black ;
  private Image start_image_green ;

  private String version() {
    return Constants.version_2_1 ;
  }

  public void start() 
  {
    addMouseListener( new MouseAdapter () {
	public void mouseReleased( MouseEvent e ) {
	  e.consume() ;
	  start_image = start_image_red ;
	  repaint() ;
	  launch() ;
	}

	public void mousePressed( MouseEvent e ) {
	  start_image = start_image_green ;
	  repaint() ;
	}

	public void mouseEntered( MouseEvent e ) {
	  start_image = start_image_black ;
	  repaint() ;
	}

	public void mouseExited( MouseEvent e ) {
	  start_image = start_image_red ;
	  repaint() ;
	}
      }) ;
  }

  public void launch() 
  {
    Dimension d = new Dimension( width, height ) ;
    harmony_frame = new Frame( "Arezzo" ) ;
    harmony_frame.setSize( d ) ;
    harmony_frame.setVisible( true ) ;
    repaint() ;

    harmony_frame.addWindowListener( new WindowAdapter() {
	public void windowClosing( WindowEvent e ) {
	  e.getWindow().dispose();
	}
      });
    Harmony h = 
      new Harmony( this,
		   d, 
		   "localhost", // getCodeBase().getHost(),
		   new Locale( "fr", "FR" ));
    harmony_frame.add( (Panel)h ) ;

    h.login( getParameter( "autoload" ),
	     getParameter( "user_name" ),
	     getParameter( "email_address" )) ;
    repaint() ;
  }

  public void stop() {
  }

  public String[][] getParameterInfo() {
    String[][] infos = {
      {"autoload", "string", "Autoload user_name,score_name"},
      {"user_name", "string", "User name"},
      {"email_adress", "string", "User's email address"},
      {"language", "string", "The language to use for the UI"},
      {"country", "string", "The country to use for the UI"}
    };
    return infos ;
  }

  public String getAppletInfo() {
    return 
      version()+", Pierre Jouvelot, CRI, MINES ParisTech" ;
  }

  private MediaTracker media_tracker = null ;

  public void paint( Graphics g ) {
    if( media_tracker == null ) {
	try {
	URL base = getCodeBase() ;
	start_image_red =
	    getImage( new URL( base+"/../playred.jpg")) ;
	start_image_green =
	    getImage( new URL( base+"/../playgreen.jpg")) ;
	start_image_black =
	    getImage( new URL( base+"/../playblack.jpg")) ;
	media_tracker = new MediaTracker( this ) ;
	media_tracker.addImage( start_image_red, 0 ) ;
	media_tracker.addImage( start_image_green, 0 ) ;
	media_tracker.addImage( start_image_black, 0 ) ;
	
	media_tracker.waitForAll() ;
	}
	catch( Exception e ) {
	    Harmony.fail( "Unable to track start image", e ) ;
	}
	start_image = start_image_red ;
    }
    g.drawImage( start_image, 0, 0, null ) ;
  }

  public final synchronized void update( Graphics g ) {
    paint( g ) ;
  }

  public void inform( String s ) {
    showStatus( version()+" - "+s ) ;
  }
}

