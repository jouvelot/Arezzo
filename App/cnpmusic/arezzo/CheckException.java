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

public class CheckException
  extends Exception 
{
  String name ; // in Sequence.default_check_options

  public CheckException( String e, String s ) {
    super( s ) ;
    name = e ;
  }
  
  public int throwMe() 
    throws CheckException 
  {
    throw this ;
  }

  boolean isReportable() {
    return !name.equals( "" ) ;
  }
}
