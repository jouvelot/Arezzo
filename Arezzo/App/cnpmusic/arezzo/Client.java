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
import java.io.* ;
import java.net.* ;
import java.security.* ;
import java.util.* ;
import java.text.* ;

class Client 
  implements Constants 
{
  private String server_name ;
  private boolean is_connected = true ;

  Client( String server ) {
    server_name = server ;
  }

  public boolean isConnected() {
    return is_connected ;
  }

  abstract class Exchanger 
  {
    Socket server ;

    ObjectOutputStream oos ;
    ObjectInputStream ois ;
    
    String command ;
    Object[] objects ;

    Exchanger( String c, Object[] objs ) 
    {
      command = c ;
      objects = objs ;
    }

    private void call() 
      throws ConnectException
    {
      try{
	server = new Socket(server_name, port);

	DataOutputStream dos = 
	  new DataOutputStream( server.getOutputStream()) ;
	dos.write( Constants.arezzo_mode ) ;
	dos.flush() ;

	oos = new ObjectOutputStream( server.getOutputStream()) ;
	ois = new ObjectInputStream( server.getInputStream()) ;

	oos.writeObject( command ) ;
	oos.writeObject( Harmony.me.user.getName()) ;

	for( int i = 0 ; i < objects.length ; i++ ) {
	  oos.writeObject( objects[i] ) ;
	}
	oos.flush();
      }
      catch( ConnectException e ) {
	  Harmony.fail( "Exchanger "+command, e ) ;
	  is_connected = false ;
	  throw e ;
      }
      catch( Exception e ) {
	  Harmony.fail( "Exchanger "+command, e ) ;
      }
    }

    protected String readString() 
      throws ConnectException
    {
      return (String)readObject() ;
    }

    protected Object readObject() 
      throws ConnectException
    {
      try {
	Object o = ois.readObject() ;

	if( o instanceof Exception ) {
	  throw (Exception)o ;
	}
	return o ;
      }
      catch( Exception e ) {
	is_connected = false ;
	throw new ConnectException( e.getMessage()) ;
      }
    }

    public Object result() 
      throws ConnectException
    {
      try {
	call() ;
	Object o = collect() ;
	boolean connected_status = is_connected ;
	
	try {// Test for possible error messages
	  while( true ) {
	    readObject() ;
	  }
	}
	catch( IOException e ) {
	  is_connected = connected_status ;
	}
	oos.close();
	ois.close() ;
	server.close();
	return o ;  
      }
      catch( Exception e ) {
	is_connected = false ;
	throw new ConnectException( e.getMessage()) ;
      }
    }

    abstract public Object collect() 
      throws ConnectException ;
  }

  String mailUser( String format, String abc, 
		   String subject, String partition_name ) 
    throws ConnectException
  {
    return (String) (new Exchanger( ".Mail "+format,
				    new Object[] {partition_name,
						  subject,
						  abc,
						  Harmony.me.user} ) {
	public Object collect() 
	  throws ConnectException
	{
	  return readString();
	}
      }).result() ;
  }

  Vector getScoreNames() 
    throws ConnectException
  {
    try {
      return (Vector) (new Exchanger( ".List Scores",
				      new Object[] {} ) {
	  public Object collect() 
	    throws ConnectException
	  {
	    Vector names = new Vector() ;
	  
	    while( true ) {
	      String s = readString() ;
	    
	      if( s.equals( ".List done" )) {
		break ;
	      }
	      int len = s.length()-Constants.notes_suffix.length() ;
	      names.addElement( s.substring( 0, len )) ;
	    }
	    return names ;
	  }
	}).result() ;
    }
    catch( ConnectException e ) {
      is_connected = false ;
      throw e ;
    }
  }

  Vector getStudyCourses( String thema ) 
    throws ConnectException
  {
    try {
      return (Vector) (new Exchanger( ".List Courses",
				      new Object[] {thema,
						    Harmony.me.locale} ) {
	  public Object collect() 
	    throws ConnectException
	  {
	    Vector courses = new Vector() ;
	  
	    while( true ) {
	      String s = readString() ;
	    
	      if( s.equals( ".List done" )) {
	 	break ;
	      }
	      courses.addElement( s ) ;
	    }
	    return courses ;
	  }
	}).result() ;
    }
    catch( ConnectException e ) {
      is_connected = false ;
      throw e ;
    }
  }

  Sequence getSequence( String filename ) 
    throws ConnectException
  {
    return (Sequence) (new Exchanger( ".Get Sequence",
				      new Object[] {filename} ){
	public Object collect() 
	  throws ConnectException
	{
	  Sequence s = new Sequence( readString()) ;
	  Object prefs = readObject() ;

	  if( prefs instanceof Options ) {
	    s.check_options = (Options)prefs ;
	    Harmony.checks_option_listener.setOptions( (Options)prefs ) ;
	  }
	  s.comments = readString() ;
	  return s ;
	}
      }).result() ;
  }

  void saveSequence( Sequence s, String filename ) 
    throws ConnectException
  {
    new Exchanger( ".Save Notes",
		   new Object[] {filename, 
				 s.sprintf( Constants.version_2_1 ), 
				 s.check_options,
				 s.comments} ) {
      public Object collect() 
      {
	return "Done" ;
      }
    }.result() ;
  }

  void saveMIDI( final Sequence s, String midiname ) 
    throws ConnectException
  {
    new Exchanger( ".Save MIDI", new Object[] {midiname} ) {
	public Object collect() 
	  throws ConnectException
	{
	  try {
	    DataOutputStream dos = 
	      new DataOutputStream( server.getOutputStream()) ;
	    s.saveMIDI( dos ) ;
	    dos.flush() ;
	    dos.close() ;
	    return "Done" ;
	  }
	  catch( Exception e ) {
	    throw new ConnectException( e.getMessage()) ;
	  }
	}
      }.result() ;
  }

  void deleteSequence( String filename ) 
    throws ConnectException
  {
    new Exchanger( ".Delete File", new Object[] {filename} ){
	public Object collect() 
	{
	  return "Done" ;
	}
      }.result() ;
  }

  String getExplanation( final String word ) 
    throws ConnectException
  {
    return (String) (new Exchanger( ".Explain Word",
				    new Object[] {word,
						  Harmony.me.locale} ) {
	public Object collect() 
	  throws ConnectException
	{
	  String rs = readString() ;

	  if( rs.equals( "" )) {
	    String ms = 
	      Harmony.me.messages.getString( "Client.NoExplanation" ) ;
	    MessageFormat mf = new MessageFormat( ms ) ;
	    return mf.format( new String[] {word}) ;
	  }
	  else {
	    return rs ;
	  }
	}
      }).result() ;
  }

  void informServer( String i ) 
    throws ConnectException
  {
    new Exchanger( ".Inform Server", new Object[] {i} ){
	public Object collect() 
	{
	  return "Done" ;
	}
      }.result() ;
  }
}
