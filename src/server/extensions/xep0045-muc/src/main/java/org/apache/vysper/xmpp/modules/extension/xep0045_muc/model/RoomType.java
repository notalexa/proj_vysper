/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.vysper.xmpp.modules.extension.xep0045_muc.model;

import java.util.EnumSet;

import org.apache.vysper.compliance.SpecCompliant;

/**
 * Describes the different room types for chats
 * @author The Apache MINA Project (dev@mina.apache.org)
 *
 */
@SpecCompliant(spec = "xep-0045", section = "4.2", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public enum RoomType {

    /**
     * A room in which the full JIDs or bare JIDs of occupants cannot be discovered by anyone, including room admins and room owners; such rooms are NOT RECOMMENDED or explicitly supported by MUC, but are possible using this protocol if a service implementation offers the appropriate configuration options; contrast with Non-Anonymous Room and Semi-Anonymous Room.
     */
    FullyAnonymous(ConfigurationType.MemberVisibility, null),

    /**
     * A room that cannot be found by any user through normal means such as searching and service discovery; antonym: Public Room.
     */
    Hidden(ConfigurationType.RoomVisibility, "muc_hidden"),

    /**
     * A room that a user cannot enter without being on the member list; antonym: Open Room.
     */
    MembersOnly(ConfigurationType.EnterPolicy,"muc_membersonly"),

    /**
     * A room in which only those with "voice" may send messages to all occupants; antonym: Unmoderated Room.
     */
    Moderated(ConfigurationType.VoicePolicy,"muc_moderated"),

    /**
     * A room in which an occupant's full JID is exposed to all other occupants, although the occupant may choose any desired room nickname; contrast with Semi-Anonymous Room and Fully-Anonymous Room.
     */
    NonAnonymous(ConfigurationType.MemberVisibility,"muc_nonanonymous"),

    /**
     * A room that anyone may enter without being on the member list; antonym: Members-Only Room.
     */
    Open(ConfigurationType.EnterPolicy,"muc_open"),

    /**
     * A room that a user cannot enter without first providing the correct password; antonym: Unsecured Room.
     */
    PasswordProtected(ConfigurationType.RoomSecurity,"muc_passwordprotected"),

    /**
     * A room that is not destroyed if the last occupant exits; antonym: Temporary Room.
     */
    Persistent(ConfigurationType.RoomLifetime,"muc_persistent"),

    /**
     * A room that can be found by any user through normal means such as searching and service discovery; antonym: Hidden Room.
     */
    Public(ConfigurationType.RoomVisibility, "muc_public"),

    /**
     * A room in which an occupant's full JID can be discovered by room admins only; contrast with Fully-Anonymous Room and Non-Anonymous Room.
     */
    SemiAnonymous(ConfigurationType.MemberVisibility,"muc_semianonymous"),

    /**
     * A room that is destroyed if the last occupant exits; antonym: Persistent Room.
     */
    Temporary(ConfigurationType.RoomLifetime,"muc_temporary"),

    /**
     * A room in which any occupant is allowed to send messages to all occupants; antonym: Moderated Room.
     */
    Unmoderated(ConfigurationType.VoicePolicy,"muc_unmoderated"),

    /**
     * A room that anyone is allowed to enter without first providing the correct password; antonym: Password-Protected Room.
     */
    Unsecured(ConfigurationType.RoomSecurity,"muc_unsecured"),

    // extra features, not covered by room types as defined in the XEP
    /**
     * Any visitor can change the room subject, not only a moderator
     */
    OpenSubject(ConfigurationType.SubjectPolicy,null),
    ModeratedSubject(ConfigurationType.SubjectPolicy,null);

	private ConfigurationType category;
    private String discoName;

    private RoomType(ConfigurationType category,String mucName) {
        this.discoName = mucName;
        this.category=category;
    }
    
    public ConfigurationType getTypeCategory() {
    	return category;
    }

    public String getDiscoName() {
        return discoName;
    }

    private static void complement(EnumSet<RoomType> types, RoomType defaultType, RoomType... antonyms) {
        if (types.contains(defaultType)) {
            return;
        }
        for (RoomType type : antonyms) {
            if (types.contains(type)) {
                // found, return
                return;
            }
        }
        // non found, add default type
        types.add(defaultType);
    }

    public static EnumSet<RoomType> complement(EnumSet<RoomType> in) {
        EnumSet<RoomType> result = EnumSet.copyOf(in);
        complement(result, RoomType.Public, RoomType.Hidden);
        complement(result, RoomType.Open, RoomType.MembersOnly);
        complement(result, RoomType.Temporary, RoomType.Persistent);
        complement(result, RoomType.Unmoderated, RoomType.Moderated);
        complement(result, RoomType.Unsecured, RoomType.PasswordProtected);
        complement(result, RoomType.NonAnonymous, RoomType.SemiAnonymous, RoomType.FullyAnonymous);

        return result;
    }

    public static void validateAntonyms(EnumSet<RoomType> types) {
        if (types.contains(RoomType.Hidden) && types.contains(RoomType.Public)) {
            throw new IllegalArgumentException("Room can not be both Hidden and Public");
        }
        if (types.contains(RoomType.MembersOnly) && types.contains(RoomType.Open)) {
            throw new IllegalArgumentException("Room can not be both MembersOnly and Open");
        }
        if (types.contains(RoomType.Temporary) && types.contains(RoomType.Persistent)) {
            throw new IllegalArgumentException("Room can not be both Temporary and Persistent");
        }
        if (types.contains(RoomType.Unmoderated) && types.contains(RoomType.Moderated)) {
            throw new IllegalArgumentException("Room can not be both Unmoderated and Moderated");
        }
        if (types.contains(RoomType.Unsecured) && types.contains(RoomType.PasswordProtected)) {
            throw new IllegalArgumentException("Room can not be both Unsecured and PasswordProtected");
        }
        if (types.contains(RoomType.NonAnonymous) && types.contains(RoomType.SemiAnonymous)) {
            throw new IllegalArgumentException("Room can not be both NonAnonymous and SemiAnonymous");
        }
        if (types.contains(RoomType.SemiAnonymous) && types.contains(RoomType.FullyAnonymous)) {
            throw new IllegalArgumentException("Room can not be both FullyAnonymous and SemiAnonymous");
        }
        if (types.contains(RoomType.NonAnonymous) && types.contains(RoomType.FullyAnonymous)) {
            throw new IllegalArgumentException("Room can not be both NonAnonymous and FullyAnonymous");
        }
    }

    public boolean includeInDisco() {
        return discoName != null;
    }

}
