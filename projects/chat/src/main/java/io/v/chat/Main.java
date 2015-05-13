// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.chat;

import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.gui.Action;
import com.googlecode.lanterna.gui.Border;
import com.googlecode.lanterna.gui.GUIScreen;
import com.googlecode.lanterna.gui.Interactable;
import com.googlecode.lanterna.gui.Window;
import com.googlecode.lanterna.gui.component.Button;
import com.googlecode.lanterna.gui.component.Panel;
import com.googlecode.lanterna.gui.component.TextArea;
import com.googlecode.lanterna.gui.component.TextBox;
import com.googlecode.lanterna.gui.dialog.MessageBox;
import com.googlecode.lanterna.gui.layout.BorderLayout;
import com.googlecode.lanterna.gui.layout.HorisontalLayout;
import com.googlecode.lanterna.gui.layout.VerticalLayout;
import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.terminal.TerminalSize;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.verror.VException;

public class Main {
    private final TextArea chatArea;
    private final TextBox sendTextBox;
    private final ChatChannel channel;
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private boolean firstMessage = true;
    private int messageCount = 0;


    public static void main(String[] args) throws VException {
        Main main = new Main();
    }

    public Main() throws VException {
        String channelName = "users/vanadium.bot@gmail.com/apps/chat/public";
        VContext ctx = V.init();
        final FocusableWindow w = new FocusableWindow("Channel: " + channelName);
        final GUIScreen screen = TerminalFacade.createGUIScreen();

        sendTextBox = new TextBox() {
            @Override
            public Result keyboardInteraction(Key key) {
                if ("Enter".equals(key.getKind().name())) {
                    doSendAction();
                    return Result.EVENT_HANDLED;
                }
                return super.keyboardInteraction(key);
            }
        };

        sendTextBox.setPreferredSize(
                new TerminalSize(screen.getScreen().getTerminalSize().getColumns() - 10, 1));
        Panel panel = new Panel();
        panel.setLayoutManager(new BorderLayout());

        Panel mainPanel = new Panel();
        mainPanel.setTitle("Chat");
        mainPanel.setBorder(new Border.Standard());
        chatArea = new TextArea();
        chatArea.setPreferredSize(
                new TerminalSize(screen.getScreen().getTerminalSize().getColumns() - 10, 25));
        mainPanel.addComponent(chatArea);
        panel.addComponent(mainPanel, BorderLayout.CENTER);

        channel = new ChatChannel(ctx, channelName, new ChatMessageListener() {
            @Override
            public void messageReceived(String whom, String message) {
                processMessage(whom, message);
            }
        });

        Panel bottomPanel = new Panel();
        bottomPanel.setTitle("Input");
        bottomPanel.setBorder(new Border.Standard());
        bottomPanel.setLayoutManager(new VerticalLayout());
        bottomPanel.addComponent(sendTextBox);
        Panel buttonPanel = new Panel();
        buttonPanel.setLayoutManager(new HorisontalLayout());
        buttonPanel.addComponent(new Button("Send", new Action() {
            @Override
            public void doAction() {
                doSendAction();
            }
        }));
        buttonPanel.addComponent(new Button("Participants", new Action() {
            @Override
            public void doAction() {
                doShowParticipantsAction();
            }
        }));
        buttonPanel.addComponent(new Button("Quit", new Action() {
            @Override
            public void doAction() {
                w.close();
                screen.getScreen().stopScreen();
                service.shutdown();
            }
        }));
        bottomPanel.addComponent(buttonPanel);

        panel.addComponent(bottomPanel, BorderLayout.BOTTOM);
        w.addComponent(panel);
        w.setFocus(sendTextBox);

        screen.getScreen().startScreen();
        channel.join();
        screen.showWindow(w);
        channel.leave();
    }

    private void doShowParticipantsAction() {
        MessageBox box;
    }

    private void processMessage(String whom, String message) {
        messageCount++;
        String text = whom + ": " + message;
        if (firstMessage) {
            firstMessage = false;
            chatArea.setLine(0, text);
        } else {
            chatArea.appendLine(whom + ": " + message);
        }
        maybeScrollChatArea();
    }

    private void maybeScrollChatArea() {
        // Lanterna doesn't support auto-scrolling text areas, nor does it give any way to change the visible text
        // area. The state it uses to represent the scroll location is all private. We work around all this using
        // reflection to update the text area's internal state... Yuck.
        try {
            Field lastSize = TextArea.class.getDeclaredField("lastSize");
            lastSize.setAccessible(true);
            int lastSizeInt = ((TerminalSize) lastSize.get(chatArea)).getRows();
            Field topScrollIndex = TextArea.class.getDeclaredField("scrollTopIndex");
            topScrollIndex.setAccessible(true);
            int scrollTopIndex = Math.max(0, messageCount - lastSizeInt);
            topScrollIndex.set(chatArea, scrollTopIndex);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Silently ignored.
        }
    }

    private void doSendAction() {
        final String text = sendTextBox.getText();
        if ("".equals(text)) {
            return;
        }
        sendTextBox.setText("");
        service.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    channel.sendMessage(text);
                } catch (VException e) {
                    e.printStackTrace(System.err);
                }

            }
        });
    }

    private static class FocusableWindow extends Window {
        public FocusableWindow(String title) {
            super(title);
        }

        @Override
        public void setFocus(Interactable newFocus) {
            super.setFocus(newFocus);
        }
    }
}
