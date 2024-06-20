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
import java.awt.*;
import java.awt.event.*;
import java.text.* ;

import com.sun.media.jsdt.*;

class Chat 
  extends Frame 
  implements Constants
{
  Label setNameLabel, sayLabel;
  Button closeButton;
  TextField typeField;
  TextArea messageArea;
    
  String user_name ;
  Session session;
  ChatClient client;
    
  Channel channel;
  String channel_chat_name = "";
  ChatListeners chat_listeners;
  Data data;
  boolean connected = false;
    
  Chat( String channel_name, String title ) {
    super();

    user_name = Harmony.me.user.getName(); 
    setTitle( title ) ;
    channel_chat_name = channel_name;
     
    addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  disconnect();
	  e.getWindow().dispose();
	}
      });
	
    Panel p;
    p = new Panel();
    p.setBackground(Color.lightGray);
    messageArea = new TextArea(10, 100);
    messageArea.setEditable(false);
    p.add(messageArea);
    add("North", p);

    p = new Panel();
    p.setBackground(Color.lightGray);
    p.setLayout( new BorderLayout()) ;
    p.add( "North",
	   new Label( Harmony.me.messages.getString( "Chat.Enter" ))) ;
    typeField = new TextField(100);
    typeField.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  if(session != null) {
	    try {
	      writeLine(typeField.getText(), channel_chat_name, 
			Harmony.me.server);
	    }
	    catch(Exception exc) {
	      Harmony.fail( "Exception while invoking Say: " + exc );
	    }
	    typeField.setText("");
	  }
	}
      });
    p.add( "South", typeField );
    add("Center", p);

    p = new Panel();
    p.setBackground(Color.lightGray);
    
    closeButton = new Button( Harmony.me.messages.getString( "Chat.Close" ));
    closeButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  disconnect();
	  dispose();
	}
      });
    p.add(closeButton);
    add("South", p);

    pack();
    setVisible( true );

    toFront() ;
    connect(channel_chat_name, Harmony.me.server);
  }

  private void connect(String channel_name, String server) {
    boolean sessionExists = false;
    URLString url = null;
      
    if(connected) {
      return;
    }
    try {
      try {
	url = URLString.createSessionURL(server,
					 sharing_port,
					 sharing_session_type,
					 chating_session_name);
	if( debug_Chat_connect ) {
	  System.err.println("Chat: connect: url: " + url);
	}
	while(!sessionExists) {
	  try {
	    if(SessionFactory.sessionExists(url)) {
	      if( debug_Chat_connect ) {
		System.err.println("Chat: connect:" + " found Session.");
	      }
	      sessionExists = true;
	    }
	  }
	  catch(NoRegistryException nre) {
	    Harmony.fail("Chat: connect:"+" no registry: sleeping.");
	    Thread.sleep(1000);
	  }
	  catch (ConnectionException ce) {
	    Harmony.fail("Chat: connect:" +
			 " connection exception: sleeping.");
	    Thread.sleep(1000);
	  }
	}
	if( debug_Chat_connect ) {
	  System.err.println("Creating a ChatMember...");
	}
	client = new ChatClient( user_name );
	session = SessionFactory.createSession(client, url, false);
		
	try {
	  String[] client_names = session.listClientNames();
	  try {
	    for(int i = 0; i < client_names.length; i++) {
	      if(client_names[i].equals(client.getName())) {
		throw new Exception();
	      }
	    }
	    session.join(client);
	  }
	  catch(Exception local_excp) {
	  }
	}
	catch(JSDTException e) {
	  Harmony.fail("Couldn't list Client names.");
	} 
	
	channel = session.createChannel(client, channel_name,
					true, true, true);
	chat_listeners = new ChatListeners(client.getName(), messageArea);
	channel.addConsumer(client, chat_listeners);
	  
	connected = true;
	repaint();
      }
      catch(Exception e) {
	System.err.print("Caught exception in ");
	System.err.println("Chat.connect: " + e);
      }
    }
    catch(Throwable th) {
      System.err.println("Chat: connect caught: " + th);
      throw new Error("Chat.connect failed: " + th);
    }
  }

  private void writeLine(String message, 
			 String channel_name, String server) {
    if(!connected) {
      if( debug_Chat_connect ) {
	System.err.println("Chat: writeLine: reconnecting...");
      }
      connect(channel_name, server);
    }
      
    try {
      data = new Data(message);
      data.setPriority(Channel.HIGH_PRIORITY);
      channel.sendToAll(client, data);
    }
    catch(ConnectionException ce) {
      Harmony.fail("Chat: writeLine: exception: " + ce);
      Harmony.fail("Chat: writeLine: disconnecting...");
      disconnect();
    }
    catch(TimedOutException ce) {
      Harmony.fail("Chat: writeLine: exception: " + ce);
      Harmony.fail("Chat: writeLine: disconnecting...");
      disconnect();
    }
    catch(Exception e) {
      Harmony.fail("Caught exception in ");
      Harmony.fail("Chat.writeLine: " + e);
    }
  }

  void disconnect() {
    if(connected == false) {
      return;
    }
    
    try {
      channel.removeConsumer(client, chat_listeners);
      channel.leave(client);
      if(channel.listConsumerNames().length == 0) {
	channel.destroy(client);
      }
      
      if(session.getChannelsJoined(client).length == 0) {
	session.leave(client);
      }
    }
    catch(Exception e) {
      Harmony.fail("Caught exception while trying to " +
		   "disconnect from chat server: " + e);
    }
    connected = false;
  }
}
