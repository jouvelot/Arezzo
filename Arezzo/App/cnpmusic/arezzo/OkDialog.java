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

class OkDialog 
  extends Dialog 
{

  private OkDialogContent content ;
  private Button input_ok, input_cancel ;

  boolean cancelled = false ;

  OkDialog( Frame f, String s, OkDialogContent c ) {
    super( f, s, true ) ;
    content = c ;
    createInterface( c.createInterface()) ;
    pack() ;
    setVisible( true ) ;
  }

  private void createInterface( Panel c ) {
    setLayout( new BorderLayout( 15, 15 )) ;
    add( "North", c ) ;

    Panel p = new Panel() ;
    p.setLayout( new FlowLayout( FlowLayout.CENTER )) ;
    input_ok = new Button( "OK" ) ;
    input_ok.addActionListener( new ActionListener () {
	public void actionPerformed( ActionEvent e ) {
	  setVisible( false ) ;
	  content.okAction() ;
	  dispose() ;
	  content.callBack() ;
	}
      }) ;
    p.add( input_ok ) ;

    input_cancel = 
      new Button( Harmony.me.messages.getString( "Harmony.Cancel" )) ;
    input_cancel.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e ) {
	setVisible( false ) ;
	content.cancelAction() ;
	cancelled = true ;
	dispose() ;
	//	content.callBack() ;
      }
    }) ;
    p.add( input_cancel ) ;

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
	e.getWindow().dispose();
	content.cancelAction() ;
	cancelled = true ;
      }
    });

    add( "South", p ) ;
    setSize( new Dimension( 250, 360 )) ;
  }
}
