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
import java.util.* ;
import com.sun.media.jsdt.* ;

public class SharingServer
    implements Constants
{
  static String server_name ;
  com.sun.media.jsdt.Client server ;

  Session sharing_session ;
  Session chating_session ;

  SharingServer( String server_name ) {
    this.server_name = server_name ;

    try {
      if( !RegistryFactory.registryExists( sharing_session_type )) {
	RegistryFactory.startRegistry( sharing_session_type ) ;
	Server.log( "SharingServer", "Starting sharing registry on "+server_name ) ;
      }
      server = new SharingClient( "Server" ) ;
      sharing_session = startSession( server, sharing_session_name ) ;
      chating_session = startSession( server, chating_session_name ) ;
    }
    catch( Exception e ) {
      System.err.println( "SharingServer/"+e ) ;
      e.printStackTrace() ;
      System.exit( 1 ) ;
    }
    Server.log( "SharingServer", "Listening for sharing on port "+sharing_port ) ;
  }

  void stop() 
    throws Exception
  {
    stopSession( sharing_session ) ;
    stopSession( chating_session ) ;
    RegistryFactory.stopRegistry( sharing_session_type ) ;
  }

  private Session startSession( com.sun.media.jsdt.Client server, String name ) 
    throws Exception 
  {
    URLString url = URLString.createSessionURL( server_name, 
						sharing_port, 
						sharing_session_type, 
						name ) ;
    Session s = SessionFactory.createSession( server, url, true ) ;
    s.createChannel( server, "Default", true, true, false ) ;
    return s ;
  }

  private void stopSession( Session s ) 
    throws Exception
  {
    Channel[] cs = s.getChannelsJoined( server ) ;

    for( int i = 0 ; i<cs.length ; i++ ) {
      cs[i].destroy( server ) ;
    }
    s.destroy( server ) ;
  }

  //

  public static Vector getChannels( String server, String session_name) 
    throws Exception 
  {
    URLString url = 
      URLString.createSessionURL( server, 
				  sharing_port, sharing_session_type, 
				  session_name) ;
    com.sun.media.jsdt.Client c = new SharingClient( "GettingChannels" ) ;
    Session s = SessionFactory.createSession( c, url, false ) ;
    Vector channel_names = new Vector() ;
    String[] channels = s.listChannelNames() ;

    for( int i = 0 ; i<channels.length ; i++ ) {
      channel_names.addElement( channels[i] ) ;
    }
    return channel_names ;
  }
}
