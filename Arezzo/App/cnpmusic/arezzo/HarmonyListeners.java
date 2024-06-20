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

class HarmonyListeners
  implements MouseListener, MouseMotionListener, 
	     KeyListener, FocusListener,
	     ActionListener, AdjustmentListener
{
  
  // Preserve the event modifiers after mousePressed, since this is
  // lost in mouseDragged.

  static boolean is_mouse_left = true ;

  static boolean mouseLeft() {
    return( is_mouse_left ) ;
  }

  static boolean mouseRight() {
    return( !is_mouse_left ) ;
  }
  // MouseListener

  public void mouseClicked( MouseEvent e ) {}

  public void mouseEntered( MouseEvent e ) {}

  public void mouseExited( MouseEvent e ) {}

  public void mousePressed( MouseEvent e ) {}

  public void mouseReleased( MouseEvent e ) {}

  // KeyListener

  public void keyPressed( KeyEvent e ) {}

  public void keyReleased( KeyEvent e ) {}

  public void keyTyped( KeyEvent e ) {}

  // MouseMotionListner

  public void mouseDragged( MouseEvent e ) {}

  public void mouseMoved( MouseEvent e ) {
  }

  // FocusListener

  public void focusGained( FocusEvent e ) {}

  public void focusLost( FocusEvent e ) {}

  // ActionListener

  public void actionPerformed( ActionEvent e ) {}

  // AdjustmentListener

  public void adjustmentValueChanged( AdjustmentEvent e ) {}
}
