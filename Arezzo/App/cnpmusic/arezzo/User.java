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
import java.io.*;
import java.util.* ;

public class User 
    implements Serializable 
{
  private String name ;
  private String quality ;
  private String email ;

  Queue errors ;

  static String guest_name = "_guest" ;
  
  static private String teacher = "Teacher" ;
  static private String student = "Student" ;
  
    User( String n, String q, String e ) {
	name = n ;
	quality = q;
	email = e ;
	errors = new LinkedList() ;
    }

  User( String n, String e ) {
    this( n, student, e ) ;
  }

  boolean isTeacher() {
    return quality.equals( teacher );
  }

  boolean isGuest() {
    return name.equals( guest_name ) ;
  }

  //

  void resetErrors() {
      errors.clear() ;
  }

  void setError( String s ) {
      errors.add( s ) ;
  }

  String mainError() {
      return (String)errors.poll() ;
  }

  //

  String getName() {
    return name;
  }

  String getQuality() {
    return quality;
  }

  String getEmail() {
    return email;
  }


}

  
