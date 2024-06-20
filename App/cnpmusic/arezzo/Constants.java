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

public interface Constants {

  // Arezzo
  
  // Original version
  static final String version_2_0 = "Arezzo_2.0" ;
  // 2.0 + cypher + new cnpmusic.com site + checks storage
  static final String version_2_1 = "Arezzo_2.1" ; 

  static final String cypher_key = "cnpmusicpj" ;
  static Color cnpmusic_light_color = 
    new Color( 204, 230, 148 ) ; // ClickNPlayMusic light green
  static Color cnpmusic_dark_color = 
    new Color( 101, 161, 107 ) ; // ClickNPlayMusic dark green

  // Arezzo servers

  static final int port = 6788 ;
  static final int command_port = 6787 ;
  static final String smtp_host = "smtp.fai.fr" ;
  static final int arezzo_mode = 1 ;
  static final int cnpmusic_mode = 2 ;

  // Harmony sharing server (jsdt)

  static final int sharing_port = 6786 ;
  static final String sharing_session_type = "socket" ;
  static final String sharing_session_name = "Harmony" ;
  static final String chating_session_name = "Chat";

  // File names on server

  static final String data_directory = "./Data/Users" ;
  static final String bin_directory = "../Bin";
  static final String applet_directory = "../../Common/Arezzo" ;

  // File names for applet

  static final String images_directory = "Images" ;
  static final String notes_suffix = ".notes" ;
  static final String abc_suffix = ".abc";
  static final String midi_suffix = ".mid" ;
  static final String mail_suffix = "_mail"+notes_suffix ;
  static final String ps_suffix = ".ps" ;
  static final String messages_file = "Bundle.MessagesBundle" ;
  static final String score_name_default = "default" ;
  static final String manual_path = "Fr/Arezzo/Manual.html" ;  
  
    
  // Debug flags

  boolean debug_SharingListeners = false ;
  boolean debug_LocalListeners = false ;
  boolean debug_Chat_connect = false ;
  boolean debug_Note = false ;
  boolean debug_Intervalle = false ;
  boolean debug_Chiffrage = false ;
}
