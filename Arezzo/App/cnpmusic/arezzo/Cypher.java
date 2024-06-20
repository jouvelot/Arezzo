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

import java.util.Vector;

public class Cypher
{
  // Vignere Encryption, with a variant for "others"
     
  static String alphabet = "abcdefghijklmnopqrstuvwxyz" ;
  static String others = "0123456789 \t|/+._\n" ;

  private int key[];
  private int alphabet_size = alphabet.length() ;
        
  public Cypher( String key ) {
    Vector values = new Vector ();
	
    for (int i = 0; i < key.length(); i++) {
      char c = Character.toLowerCase( key.charAt( i )) ;
  
      if( Character.isLetter( c )) {
	values.addElement (new Integer((int) (c - 'a')));	   
      }
    }

    this.key = new int[values.size()];

    for (int i = 0; i < values.size (); i++)
      this.key[i] = ((Integer) values.elementAt(i)).intValue ();
  }
   
  // direction = true = crypt, false = uncrypt

  public String go( String text, boolean direction) {
    String r = "";
    char c;
    int j = 0;

    for (int i = 0; i < text.length(); i++) {
      c = text.charAt(i);

      if( others.indexOf( c ) != -1 ) {
	int new_i = (others.indexOf( c)+ ((direction) ? 1 : -1)*3) ;
	int l = others.length() ;

	r += others.charAt( (new_i >= l) ? new_i-l :
			    (new_i<0) ? new_i+l : new_i ) ;
      }
      else {
	int shift = (Character.isUpperCase( c )) ? 'A' : 'a';
	int z = c - shift;
	if (direction) 
	  z = (z + key[j % key.length] + alphabet_size) % alphabet_size;
	else 
	  z = (z - key[j % key.length] + alphabet_size) % alphabet_size;
	c = (char) (z + shift); 
	r = r + c;
	j++;
      }
    }
    return r;
  }

  public static void main( String args[] ) {
    Cypher c = new Cypher( "test" ) ;
    System.out.println( "crypt : "+c.go( args[ 0 ], true )) ;

    Cypher d = new Cypher( "test" ) ;
    System.out.println( "decrypt : "+d.go( c.go( args[ 0 ], true ), false )) ;
    System.out.println( "decrypt : "+d.go( c.go( args[ 0 ]+"\nfoo o", true ), false )) ;
    

  }
}
