// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.chat;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.googlecode.lanterna.gui.Action;
import com.googlecode.lanterna.gui.GUIScreen;
import com.googlecode.lanterna.gui.Window;
import com.googlecode.lanterna.gui.component.Button;
import com.googlecode.lanterna.gui.component.Panel;
import com.googlecode.lanterna.gui.component.TextArea;
import com.googlecode.lanterna.gui.layout.BorderLayout;
import com.googlecode.lanterna.gui.layout.HorisontalLayout;

import java.util.List;

class ParticipantsPanel extends Panel {
    private final TextArea participantTextArea;

    public ParticipantsPanel(ChatChannel channel) {
        super();
        addComponent(participantTextArea = new TextArea());
    }

    public void setParticipants(List<Participant> participants) {
        participantTextArea.clear();
        boolean first = true;
        Function<Participant, String> nameFunction = new Function<Participant, String>() {
            @Override
            public String apply(Participant input) {
                return input.getName();
            }
        };
        for (String participant : Ordering.natural().sortedCopy(
                Iterables.transform(participants, nameFunction))) {
            if (first) {
                participantTextArea.setLine(0, participant);
                first = false;
            } else {
                participantTextArea.appendLine(participant);
            }
        }
    }

    public void showParticipantsWindow(GUIScreen screen) {
        final Window w = new Window("Participants");
        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new BorderLayout());
        mainPanel.addComponent(this, BorderLayout.CENTER);

        Panel buttonPanel = new Panel();
        buttonPanel.setLayoutManager(new HorisontalLayout());
        buttonPanel.addComponent(new Button("Close", new Action() {
            @Override
            public void doAction() {
                w.close();
            }
        }));
        mainPanel.addComponent(buttonPanel, BorderLayout.BOTTOM);
        w.addComponent(mainPanel);
        screen.showWindow(w, GUIScreen.Position.CENTER);
    }
}
