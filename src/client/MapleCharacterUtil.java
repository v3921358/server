/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client;

import constants.GameConstants;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import tools.Pair;
import java.util.regex.Pattern;

import database.DatabaseConnection;

public class MapleCharacterUtil {

    private static final Pattern namePattern = Pattern.compile("[a-zA-Z0-9_-]{3,12}");
    private static final Pattern petPattern = Pattern.compile("[a-zA-Z0-9_-]{4,12}");

    public static final boolean canCreateChar(final String name) {
        if (getIdByName(name) != -1 || !isEligibleCharName(name)) {
            return false;
        }
        return true;
    }

    public static final boolean isEligibleCharName(final String name) {
        if (name.length() > 15) {
            return false;
        }
        if (name.length() < 2) {
            return false;
        }
        return true;
    }

    public static final boolean canChangePetName(final String name) {
        if (petPattern.matcher(name).matches()) {
            return true;
        }
        return false;
    }

    public static final String makeMapleReadable(final String in) {
        String wui = in.replace('I', 'i');
        wui = wui.replace('l', 'L');
        wui = wui.replace("rn", "Rn");
        wui = wui.replace("vv", "Vv");
        wui = wui.replace("VV", "Vv");
        return wui;
    }

    public static final int getIdByName(final String name) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id FROM characters WHERE name = ?");
            ps.setString(1, name);
            final ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            final int id = rs.getInt("id");
            rs.close();
            ps.close();

            return id;
        } catch (SQLException e) {
            System.err.println("error 'getIdByName' " + e);
        }
        return -1;
    }

    public static final boolean PromptPoll(final int accountid) {
        PreparedStatement ps = null;
        ResultSet rs = null;

        boolean prompt = false;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("SELECT * from game_poll_reply where AccountId = ?");
            ps.setInt(1, accountid);

            rs = ps.executeQuery();
            prompt = rs.next() ? false : true;
        } catch (SQLException e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
            }
        }
        return prompt;
    }

    public static final boolean SetPoll(final int accountid, final int selection) {
        if (!PromptPoll(accountid)) { // Hacking OR spamming the db.
            return false;
        }

        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO game_poll_reply (AccountId, SelectAns) VALUES (?, ?)");
            ps.setInt(1, accountid);
            ps.setInt(2, selection);

            ps.execute();
        } catch (SQLException e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
            }
        }
        return true;
    }

    //id accountid gender
    public static Pair<Integer, Pair<Integer, Integer>> getInfoByName(String name, int world) {
        try {

            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE name = ? AND world = ?");
            ps.setString(1, name);
            ps.setInt(2, world);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }
            Pair<Integer, Pair<Integer, Integer>> id = new Pair<Integer, Pair<Integer, Integer>>(rs.getInt("id"), new Pair<Integer, Integer>(rs.getInt("accountid"), rs.getInt("gender")));
            rs.close();
            ps.close();
            return id;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void sendNote(String to, String name, String msg, int fame) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`, `gift`) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, to);
            ps.setString(2, name);
            ps.setString(3, msg);
            ps.setLong(4, System.currentTimeMillis());
            ps.setInt(5, fame);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Unable to send note" + e);
        }
    }
}
