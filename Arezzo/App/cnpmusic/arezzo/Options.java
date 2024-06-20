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
import java.lang.* ;
import java.util.* ;

class Options
  extends Hashtable
{
  String[] names ;

  static Boolean option_true = new Boolean( true ) ;
  static Boolean option_false = new Boolean( false ) ;

  Options() {
    super() ;
  }

  Options( String[] names, Object value ) {
    super() ;
    this.names = names ;
    init( value ) ;
  }

  Options( Vector v, Object value ) {
    super() ;
    names = new String[v.size()] ;
    v.copyInto( names ) ;
    init( value ) ;
  }

  void init( Object value ) {
    for( int i = 0 ; i < names.length ; i++ ) {
      put( names[i], value ) ;
    }
  }

  void set( String n ) {
    put( n, new Boolean( true )) ;
  }

  void reset( String n ) {
    put( n, new Boolean( false )) ;
  }

  boolean isTrue( String n ) {
    return ((Boolean)get( n )).booleanValue() ;
  }

  boolean isFalse( String n ) {
    return !isTrue( n ) ;
  }
}
