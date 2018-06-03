/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * This file copyright (C) 2009-2015,2017-2018 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2012-2013 Paul Bilnoski <paul@bilnoski.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/
package soc.client;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;

import soc.client.SOCPlayerClient.GameAwtDisplay;
import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.game.SOCScenario;
import soc.game.SOCVersionedItem;
import soc.message.SOCMessage;
import soc.message.SOCStatusMessage;
import soc.util.SOCGameList;
import soc.util.SOCStringManager;
import soc.util.Version;

/**
 * This is the dialog for a game's name and options to set, along with the client's
 * user preferences such as {@link SOCPlayerClient#PREF_SOUND_ON}
 * and per-game preferences such as {@link SOCPlayerInterface#PREF_SOUND_MUTE}.
 * When "Create" button is clicked, validates fields and calls
 * {@link SOCPlayerClient.GameAwtDisplay#askStartGameWithOptions(String, boolean, Map, Map)}.
 *<P>
 * Also used for showing a game's options (read-only) during game play.
 *<P>
 * Changes to the {@code PREF_SOUND_ON} or {@code PREF_SOUND_MUTE} checkboxes
 * take effect immediately so the user can mute sound effects with minimal frustration.
 *<P>
 * If this window already exists and you'd like to make it topmost,
 * call {@link #setVisible(boolean)} instead of {@link #requestFocus()}.
 *<P>
 * Game option "SC" (Scenarios) gets special rendering. Internally it's {@link SOCGameOption#OTYPE_STR},
 * but it's presented as a checkbox and {@link Choice}. When a scenario is picked in the Choice,
 * related options are updated by "SC"'s {@link SOCGameOption.ChangeListener}.
 *<P>
 * This class also contains the "Scenario Info" popup window, called from
 * this dialog's Scenario Info button, and from {@link SOCPlayerInterface}
 * when first joining a game with a scenario.
 * See {@link #showScenarioInfoDialog(SOCScenario, Map, int, SOCPlayerClient.GameAwtDisplay, Frame)}.
 *
 * @author Jeremy D Monin &lt;jeremy@nand.net&gt;
 * @since 1.1.07
 */
@SuppressWarnings("serial")
public class NewGameOptionsFrame extends Frame
    implements ActionListener, KeyListener, ItemListener, TextListener, MouseListener
{
    // See initInterfaceElements() for most of the UI setup.
    // See clickCreate() for method which handles game setup after options have been chosen.

    /**
     * Maximum range (min-max value) for integer-type options
     * to be rendered using a value popup, instead of a textfield.
     * @see #initOption_int(SOCGameOption)
     */
    public static final int INTFIELD_POPUP_MAXRANGE = 21;

    /**
     * Game's interface if known, or {@code null} for a new game.
     * Used for updating settings like {@link SOCPlayerInterface#isSoundMuted()}.
     * @see #localPrefs
     * @see #forNewGame
     * @since 1.2.00
     */
    private final SOCPlayerInterface pi;

    private final SOCPlayerClient.GameAwtDisplay gameDisplay;

    /** should this be sent to the remote tcp server, or local practice server? */
    private final boolean forPractice;

    /**
     * Map of local client preferences for a new or current game.
     * Same keys and values as {@link SOCPlayerInterface} constructor's
     * {@code localPrefs} parameter.
     * @since 1.2.00
     */
    private final HashMap<String, Object> localPrefs;

    /**
     * Is this NGOF used to set options for a new game, not to show them for an existing one?
     * If true, {@link #pi} == {@code null}.
     * @since 2.0.00
     */
    private final boolean forNewGame;

    /** is this for display only? True if shown for an existing game (not a new game). */
    private final boolean readOnly;

    /** Contains this game's {@link SOCGameOption}s, or null if none.
     *  Unknowns (OTYPE_UNKNOWN) are removed in initInterface_options.
     *<P>
     * The opts' values are updated from controls when the user hits the Create Game button,
     * and sent to the server to create the game.  If there are {@link SOCGameOption.ChangeListener}s,
     * they are updated as soon as the user changes them in the controls, then re-updated when
     * Create is hit.
     * @see #readOptsValuesFromControls(boolean)
     */
    private Map<String, SOCGameOption> opts;

    /** Key = AWT control; value = {@link SOCGameOption} within {@link #opts}. Empty if opts is null.  */
    private Map<Component, SOCGameOption> controlsOpts;

    /**
     * AWT control for each gameopt, for handling {@link SOCGameOption#refreshDisplay()}
     * if called by {@link SOCGameOption.ChangeListener}s.
     * Key = option key; value = Component.
     * Null if {@link #readOnly}.
     * For game options with 2 input controls (OTYPE_INTBOOL, OTYPE_ENUMBOOL),
     * the TextField/Choice is found here, and the boolean Checkbox is found in {@link #boolOptCheckboxes}.
     * The scenario dropdown (option {@code "SC"}) uses a {@code JComboBox} control holding
     * {@link SOCScenario} objects and the string "(none)".
     * @since 1.1.13
     * @see #fireOptionChangeListener(soc.game.SOCGameOption.ChangeListener, SOCGameOption, Object, Object)
     */
    private Map<String, Component> optsControls;

    /** Key = {@link SOCVersionedItem#key SOCGameOption.key}; value = {@link Checkbox} if bool/intbool option.
      * Empty if none, null if readOnly.
      * Used to quickly find an option's associated checkbox.
      */
    private Map<String, Checkbox> boolOptCheckboxes;

    /**
     * Scenario info for {@link #scenDropdown}, if {@link #opts} contains the {@code "SC"} game option, or null.
     * Initialized from {@link SOCScenario#getAllKnownScenarios()} during
     * {@link #initInterface_Options(JPanel, GridBagLayout, GridBagConstraints)},
     * which is called after any server negotiations.
     * @since 2.0.00
     */
    private Map<String, SOCScenario> allSc;

    /**
     * Scenario choice dropdown if {@link #opts} contains the {@code "SC"} game option, or null.
     * When an item is selected, {@link #actionPerformed(ActionEvent)} reacts specially for this control
     * to update {@code "SC"} within {@link #opts} and enable/disable {@link #scenInfo}.
     * @since 2.0.00
     */
    private JComboBox scenDropdown;

    /**
     * Scenario Info button, for info window about {@link #scenDropdown}'s selected scenario, or null.
     * @see #clickScenarioInfo()
     * @since 2.0.00
     */
    private Button scenInfo;

    /** Create Game button; null if {@link #readOnly} */
    private Button create;
    /** Cancel button; text is "OK" if {@link #readOnly} */
    private Button cancel;
    private TextField gameName;
    /** msgText is null if readOnly */
    private TextField msgText;

    // // TODO refactor; these are from connectorprac panel
    private static final Color NGOF_BG = new Color(97, 175, 113);  // green tone #61AF71
    private static final Color HEADER_LABEL_BG = new Color(220,255,220);
    private static final Color HEADER_LABEL_FG = Color.BLACK;

    /**
     * i18n text strings; will use same locale as SOCPlayerClient's string manager.
     * Localized option names are requested from the server when client locale isn't en_US.
     * @since 2.0.00
     */
    private static final SOCStringManager strings = SOCStringManager.getClientManager();

    /**
     * Creates a new NewGameOptionsFrame.
     * Once created, resets the mouse cursor from hourglass to normal, and clears main panel's status text.
     *<P>
     * See also convenience method
     * {@link #createAndShow(SOCPlayerInterface, GameAwtDisplay, String, Map, boolean, boolean)}.
     *
     * @param pi  Interface of existing game, or {@code null} for a new game.
     *     Used for updating settings like {@link SOCPlayerInterface#isSoundMuted()}.
     * @param gd      Game display interface
     * @param gaName   Name of existing game,
     *                 or null for new game; will be blank or (forPractice)
     *                 to use {@link SOCPlayerClient#DEFAULT_PRACTICE_GAMENAME}.
     * @param opts     Set of {@link SOCGameOption}s; its values will be changed when "New Game" button
     *                 is pressed, so the next OptionsFrame will default to the values the user has chosen.
     *                 To preserve them, call {@link SOCGameOption#cloneOptions(Map)} beforehand.
     *                 Null if server doesn't support game options.
     *                 Unknown options ({@link SOCGameOption#OTYPE_UNKNOWN}) will be removed.
     *                 If not <tt>readOnly</tt>, each option's {@link SOCGameOption#userChanged userChanged}
     *                 flag will be cleared, to reset status from any previously shown NewGameOptionsFrame.
     * @param forPractice For making a new game: Will the game be on local practice server, vs remote tcp server?
     * @param readOnly    Is this display-only (for use during a game), or can it be changed (making a new game)?
     */
    public NewGameOptionsFrame
        (final SOCPlayerInterface pi, GameAwtDisplay gd, String gaName,
         Map<String, SOCGameOption> opts, boolean forPractice, boolean readOnly)
    {
        super( readOnly
                ? (strings.get("game.options.title", gaName))
                : (forPractice
                    ? strings.get("game.options.title.newpractice")
                    : strings.get("game.options.title.new")));

        // Uses default BorderLayout, for simple stretching when frame is resized

        this.pi = pi;
        this.gameDisplay = gd;
        SOCPlayerClient cli = gd.getClient();
        forNewGame = (gaName == null);
        this.opts = opts;
        localPrefs = new HashMap<String, Object>();
        this.forPractice = forPractice;
        this.readOnly = readOnly;
        controlsOpts = new HashMap<Component, SOCGameOption>();
        if (! readOnly)
        {
            optsControls = new HashMap<String, Component>();
            boolOptCheckboxes = new HashMap<String, Checkbox>();
        }
        if ((gaName == null) && forPractice)
        {
            if (cli.numPracticeGames == 0)
                gaName = cli.DEFAULT_PRACTICE_GAMENAME;
            else
                gaName = cli.DEFAULT_PRACTICE_GAMENAME + " " + (1 + cli.numPracticeGames);
        }

        // same Frame setup as in SOCPlayerClient.main
        setBackground(NGOF_BG);
        setForeground(Color.black);
        setLocationByPlatform(true);

        addKeyListener(this);

        initInterfaceElements(gaName);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { clickCancel(); }
            });

        /**
         * setup is complete; reset mouse cursor from hourglass to normal
         * (was set to hourglass before calling this constructor)
         */
        gd.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        gd.status.setText("");  // clear "Talking to server..."
    }

    /**
     * Creates and shows a new NewGameOptionsFrame.
     * Once created, resets the mouse cursor from hourglass to normal, and clears main panel's status text.
     * See {@link #NewGameOptionsFrame(SOCPlayerInterface, SOCPlayerClient.GameAwtDisplay, String, Map, boolean, boolean) constructor}
     * for notes about <tt>opts</tt> and other parameters.
     * @param pi  Interface of existing game, or {@code null} for a new game; see constructor
     * @param gaName  Name of existing game, or {@code null} to show options for a new game;
     *     see constructor for details
     * @return the new frame
     */
    public static NewGameOptionsFrame createAndShow
        (SOCPlayerInterface pi, GameAwtDisplay cli, String gaName,
         Map<String, SOCGameOption> opts, boolean forPractice, boolean readOnly)
    {
        NewGameOptionsFrame ngof = new NewGameOptionsFrame(pi, cli, gaName, opts, forPractice, readOnly);
        ngof.pack();
        ngof.setVisible(true);

        return ngof;
    }

    /**
     * Interface setup for constructor. Assumes frame is using BorderLayout.
     * Most elements are part of a sub-panel occupying most of this Frame, and using GridBagLayout.
     * Fills {@link #localPrefs}.
     */
    private void initInterfaceElements(final String gaName)
    {
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();

        final JPanel bp = new JPanel(gbl);  // Actual button panel
        bp.setBorder(new EmptyBorder(4, 4, 4, 4));  // need padding around edges, because panel fills the frame
        bp.setForeground(getForeground());
        bp.setBackground(NGOF_BG);  // If this is omitted, firefox 3.5+ applet uses themed bg-color (seen OS X)

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;  // stretch with frame resize

        if ((! readOnly) && (opts != null))
        {
            msgText = new TextField(strings.get("game.options.prompt"));  // "Choose options for the new game."
            msgText.setEditable(false);
            msgText.setForeground(LABEL_TXT_COLOR);
            msgText.setBackground(getBackground());
            add(msgText, BorderLayout.NORTH);
        }

        /**
         * Interface setup: Game name
         */
        Label L;

        L = new Label(strings.get("game.options.name"));
        L.setAlignment(Label.RIGHT);
        L.setBackground(HEADER_LABEL_BG);
        L.setForeground(HEADER_LABEL_FG);
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbl.setConstraints(L, gbc);
        bp.add(L);

        gameName = new TextField(20);
        if (gaName != null)
            gameName.setText(gaName);
        if (readOnly)
        {
            gameName.setEnabled(false);
        } else {
            gameName.addTextListener(this);    // Will enable buttons when field is not empty
            gameName.addKeyListener(this);     // for ESC/ENTER
        }
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbl.setConstraints(gameName, gbc);
        bp.add(gameName);

        /**
         * Interface setup: Game Options, user's client preferences, per-game local preferences
         */
        initInterface_Options(bp, gbl, gbc);

        /**
         * Interface setup: Buttons
         * Bottom row, centered in middle
         */
        JPanel btnPan = new JPanel();
        btnPan.setBorder(new EmptyBorder(4, 2, 0, 2));  // padding between option rows, buttons

        if (readOnly)
        {
            cancel = new Button(strings.get("base.ok"));
            cancel.setEnabled(true);
        } else {
            cancel = new Button(strings.get("base.cancel"));
            cancel.addKeyListener(this);  // for win32 keyboard-focus
        }
        cancel.addActionListener(this);
        btnPan.add(cancel);

        if (! readOnly)
        {
            create = new Button(strings.get("game.options.oknew"));  // "Create Game"
            AskDialog.styleAsDefault(create);
            create.addActionListener(this);
            create.addKeyListener(this);
            create.setEnabled(! readOnly);
            if ((gaName == null) || (gaName.length() == 0))
                create.setEnabled(false);  // Will enable when gameName not empty
            btnPan.add(create);
        }

        add(btnPan, BorderLayout.SOUTH);

        // Final assembly setup
        bp.validate();
        add(bp, BorderLayout.CENTER);
    }

    private final static Color LABEL_TXT_COLOR = new Color(252, 251, 243); // off-white

    /**
     * Interface setup: {@link SOCGameOption}s, user's client preferences, per-game local preferences.
     * One row per option, except for 3-letter options which group with 2-letter ones.
     * Boolean checkboxes go on the left edge; text and int/enum values are to right of checkboxes.
     *<P>
     * When showing options to create a new game, option keys starting with '_' are hidden
     * unless the player nickname is "debug".  This prevents unwanted changes to those options,
     * which are set at the server during game creation.  When the options are shown read-only
     * during a game, these options are shown and not hidden.
     *<P>
     * Options which have {@link SOCGameOption#FLAG_INTERNAL_GAME_PROPERTY} are always hidden.
     * If not {@link #readOnly}, they're removed from opts.  Unknown opts are always removed.
     *<P>
     * This is called from constructor, so this is a new NGOF being shown.
     * If not read-only, clear {@link SOCGameOption#userChanged} flag for
     * each option in {@link #opts}.
     *<P>
     * If options are null, put a label with "This server version does not support game options" (localized).
     *<P>
     * Sets up local preferences for the client by calling
     * {@link #initInterface_UserPrefs(JPanel, GridBagLayout, GridBagConstraints)}.
     */
    private void initInterface_Options(JPanel bp, GridBagLayout gbl, GridBagConstraints gbc)
    {
        final boolean hideUnderscoreOpts = (! readOnly) && (! gameDisplay.nick.getText().equalsIgnoreCase("debug"));

        Label L;

        if (opts == null)
        {
            L = new Label(strings.get("game.options.not"));  // "This server version does not support game options."
            L.setForeground(LABEL_TXT_COLOR);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbl.setConstraints(L, gbc);
            bp.add(L);

            initInterface_UserPrefs(bp, gbl, gbc);

            return;  // <---- Early return: no options ----
        }
        else if (! readOnly)
        {
            for (SOCGameOption opt : opts.values())
                opt.userChanged = false;  // clear flag from any previously shown NGOF
        }

        if (opts.containsKey("SC"))
            allSc = SOCScenario.getAllKnownScenarios();

        gbc.anchor = GridBagConstraints.WEST;

        // Look for options that should be on the same
        // line as other options (based on key length)
        // instead of at the start of a line.
        // TODO: for now these are on subsequent lines
        //   instead of sharing the same line.
        HashMap<String,String> sameLineOpts = new HashMap<String,String>();  // key=on-same-line opt, value=opt to start line
        for (final String kf3 : opts.keySet())
        {
            if (kf3.length() <= 2)
                continue;
            final String kf2 = kf3.substring(0, 2);
            if (opts.containsKey(kf2))
                sameLineOpts.put(kf3, kf2);
        }

        // Sort and lay out options; remove unknowns and internal-onlys from opts.
        // TreeSet sorts game options by description, using gameopt.compareTo.
        // The array lets us remove from opts without disrupting an iterator.
        SOCGameOption[] optArr = new TreeSet<SOCGameOption>(opts.values()).toArray(new SOCGameOption[0]);
        for (int i = 0; i < optArr.length; ++i)
        {
            SOCGameOption op = optArr[i];
            if (op.optType == SOCGameOption.OTYPE_UNKNOWN)
            {
                opts.remove(op.key);
                continue;  // <-- Removed, Go to next entry --
            }

            if (op.hasFlag(SOCGameOption.FLAG_INTERNAL_GAME_PROPERTY))
            {
                if (! readOnly)
                    opts.remove(op.key);  // ignore internal-property options when requesting new game from client
                continue;  // <-- Don't show internal-property options
            }

            if (op.key.charAt(0) == '_')
            {
                if (hideUnderscoreOpts)
                    continue;  // <-- Don't show options starting with '_'

                if ((allSc != null) && allSc.containsKey(op.key.substring(1)))
                    continue;  // <-- Don't show options which are scenario names (use SC dropdown to pick at most one)
            }

            if (sameLineOpts.containsKey(op.key))
                continue;  // <-- Shares a line, Go to next entry --
            final boolean sharesLine = sameLineOpts.containsValue(op.key);

            initInterface_OptLine(op, bp, gbl, gbc);
            if (sharesLine)
            {
                // Group them under this one.
                // TODO group on same line, not following lines, if there's only 1.
                for (final String kf3 : sameLineOpts.keySet())
                {
                    final String kf2 = sameLineOpts.get(kf3);
                    if ((kf2 == null) || ! kf2.equals(op.key))
                        continue;  // <-- Goes with a a different option --

                    final SOCGameOption op3 = opts.get(kf3);
                    if (op3 != null)
                        initInterface_OptLine(op3, bp, gbl, gbc);
                }
            }

        }  // for(opts)

        initInterface_UserPrefs(bp, gbl, gbc);
    }

    /**
     * Set up one game option in one line of the panel.
     * Based on the option type, create the appropriate AWT component and call
     * {@link #initInterface_Opt1(SOCGameOption, Component, boolean, boolean, JPanel, GridBagLayout, GridBagConstraints)}.
     *<P>
     * Special handling: Scenario (option {@code "SC"}) gets a checkbox, label, dropdown, and a second line with
     * an Info button. (Sets {@link #scenDropdown}, {@link #scenInfo}).
     *
     * @param op  Option data
     * @param bp  Add to this panel
     * @param gbl Use this layout
     * @param gbc Use these constraints
     */
    private void initInterface_OptLine
        (SOCGameOption op, JPanel bp, GridBagLayout gbl, GridBagConstraints gbc)
    {
        if (op.key.equals("SC"))
        {
            // special handling: Scenario
            if ((allSc == null) || allSc.isEmpty())
                return;

            int i = 0, sel = 0;

            JComboBox jcb = new JComboBox();
            jcb.addItem(strings.get("base.none.parens"));  // "(none)" is item 0 in dropdown

            Collection<SOCScenario> scens = allSc.values();
            if (! readOnly)
            {
                // Sort by description.
                // Don't sort if readOnly and thus dropdown not enabled, probably not browsable.

                ArrayList<SOCScenario> sl = new ArrayList<SOCScenario>(scens);
                Collections.sort(sl, new Comparator<SOCScenario>() {
                    // This method isn't part of SOCScenario because that class already has
                    // equals and compareTo methods comparing keys, not descriptions

                    public int compare(SOCScenario a, SOCScenario b)
                    {
                        return a.getDesc().compareTo(b.getDesc());
                    }
                });
                scens = sl;
            }

            for (final SOCScenario sc : scens)
            {
                ++i;
                jcb.addItem(sc);  // sc.toString() == sc.desc
                if (sc.key.equals(op.getStringValue()))
                    sel = i;
            }
            if (sel != 0)
            {
                jcb.setSelectedIndex(sel);
                op.setBoolValue(true);
            }

            scenDropdown = jcb;
            initInterface_Opt1(op, jcb, true, true, bp, gbl, gbc);
                // adds jcb, and a checkbox which will toggle this OTYPE_STR's op.boolValue

            if ((! readOnly) || opts.containsKey("SC"))
            {
                // 2nd line: right-justified "Scenario Info..." button

                Label blank = new Label();
                gbc.gridwidth = 1;
                gbc.weightx = 0;
                gbl.setConstraints(blank, gbc);
                bp.add(blank);
                scenInfo = new Button(strings.get("game.options.scenario.info_btn"));  // "Scenario Info..."
                scenInfo.addActionListener(this);
                scenInfo.addKeyListener(this);
                scenInfo.setEnabled(sel != 0);  // disable if "(none)" is selected scenario option

                gbc.gridwidth = GridBagConstraints.REMAINDER;
                final int oldAnchor = gbc.anchor, oldFill = gbc.fill;
                gbc.fill = GridBagConstraints.NONE;
                gbc.anchor = GridBagConstraints.EAST;
                gbl.setConstraints(scenInfo, gbc);
                bp.add(scenInfo);
                gbc.fill = oldFill;
                gbc.anchor = oldAnchor;
            }

            return;
        }

        switch (op.optType)  // OTYPE_*
        {
        case SOCGameOption.OTYPE_BOOL:
            {
                Checkbox cb = new Checkbox();
                initInterface_Opt1(op, cb, true, false, bp, gbl, gbc);
                cb.addItemListener(this);
            }
            break;

        case SOCGameOption.OTYPE_INT:
        case SOCGameOption.OTYPE_INTBOOL:
            {
                final boolean hasCheckbox = (op.optType == SOCGameOption.OTYPE_INTBOOL);
                initInterface_Opt1(op, initOption_int(op), hasCheckbox, true, bp, gbl, gbc);
            }
            break;

        case SOCGameOption.OTYPE_ENUM:
        case SOCGameOption.OTYPE_ENUMBOOL:
            // Choice (popup menu)
            {
                final boolean hasCheckbox = (op.optType == SOCGameOption.OTYPE_ENUMBOOL);
                initInterface_Opt1(op, initOption_enum(op), hasCheckbox, true, bp, gbl, gbc);
            }
            break;

        case SOCGameOption.OTYPE_STR:
        case SOCGameOption.OTYPE_STRHIDE:
            {
                int txtwid = op.maxIntValue;  // used as max length
                if (txtwid > 20)
                    txtwid = 20;
                final boolean doHide = (op.optType == SOCGameOption.OTYPE_STRHIDE);
                String txtcontent = (doHide ? "" : op.getStringValue());
                TextField txtc = new TextField(txtcontent, txtwid);
                if (doHide)
                {
                    if (SOCPlayerClient.isJavaOnOSX)
                        txtc.setEchoChar('\u2022');  // round bullet (option-8)
                    else
                        txtc.setEchoChar('*');
                }
                if (! readOnly)
                {
                    txtc.addKeyListener(this);  // for ESC/ENTER
                    txtc.addTextListener(this); // for gameopt.ChangeListener and userChanged
                }
                initInterface_Opt1(op, txtc, false, false, bp, gbl, gbc);
            }
            break;

            // default: unknown, ignore; see above
        }
    }

    /**
     * Add one GridBagLayout row with this game option (component and label(s)).
     * The option's descriptive text may have "#" as a placeholder for where
     * int/enum value is specified (IntTextField or Choice-dropdown).
     * @param op  Option data
     * @param oc  Component with option choices (popup menu, textfield, etc).
     *            If oc is a {@link TextField} or {@link Choice}, and hasCB,
     *            changing the component's value will set the checkbox.
     *            <tt>oc</tt> will be added to {@link #optsControls} and {@link #controlsOpts}.
     * @param hasCB  Add a checkbox?  If oc is {@link Checkbox}, set this true;
     *            it won't add a second checkbox.
     *            The checkbox will be added to {@link #boolOptCheckboxes} and {@link #controlsOpts}.
     * @param allowPH  Allow the "#" placeholder within option desc?
     * @param bp  Add to this panel
     * @param gbl Use this layout
     * @param gbc Use these constraints; gridwidth will be set to 1 and then REMAINDER
     */
    private void initInterface_Opt1(SOCGameOption op, Component oc,
            boolean hasCB, boolean allowPH,
            JPanel bp, GridBagLayout gbl, GridBagConstraints gbc)
    {
        Label L;

        // reminder: same gbc widths/weights are used in initInterface_UserPrefs/initInterface_Pref1

        gbc.gridwidth = 1;
        gbc.weightx = 0;
        if (hasCB)
        {
            Checkbox cb;
            if (oc instanceof Checkbox)
                cb = (Checkbox) oc;
            else
                cb = new Checkbox();
            controlsOpts.put(cb, op);
            cb.setState(op.getBoolValue());
            cb.setEnabled(! readOnly);
            gbl.setConstraints(cb, gbc);
            bp.add(cb);
            if (! readOnly)
            {
                boolOptCheckboxes.put(op.key, cb);
                cb.addItemListener(this);  // for op's ChangeListener and userChanged
            }
        } else {
            L = new Label();  // to fill checkbox's column
            gbl.setConstraints(L, gbc);
            bp.add(L);
        }

        final String opDesc = op.getDesc();
        final int placeholderIdx = allowPH ? opDesc.indexOf('#') : -1;
        Panel optp = new Panel();  // with FlowLayout
        try
        {
            FlowLayout fl = (FlowLayout) (optp.getLayout());
            fl.setAlignment(FlowLayout.LEFT);
            fl.setVgap(0);
            fl.setHgap(0);
        }
        catch (Throwable fle) {}

        // Any text to the left of placeholder in op.desc?
        if (placeholderIdx > 0)
        {
            L = new Label(opDesc.substring(0, placeholderIdx));
            L.setForeground(LABEL_TXT_COLOR);
            optp.add(L);
            if (hasCB && ! readOnly)
            {
                controlsOpts.put(L, op);
                L.addMouseListener(this);  // Click label to toggle checkbox
            }
        }

        // TextField or Choice or JComboBox at placeholder position
        if (! (oc instanceof Checkbox))
        {
            controlsOpts.put(oc, op);
            oc.setEnabled(! readOnly);
            optp.add(oc);
            if (hasCB && ! readOnly)
            {
                if (oc instanceof TextField)
                {
                    ((TextField) oc).addTextListener(this);  // for enable/disable
                    ((TextField) oc).addKeyListener(this);   // for ESC/ENTER
                }
                else if (oc instanceof Choice)
                {
                    ((Choice) oc).addItemListener(this);  // for related cb, and op.ChangeListener and userChanged
                }
                else if (oc instanceof JComboBox)
                {
                    ((JComboBox) oc).addActionListener(this);  // for related cb, and op.ChangeListener and userChanged
                }
            }
        }
        if (! readOnly)
            optsControls.put(op.key, oc);

        // Any text to the right of placeholder?  Also creates
        // the text label if there is no placeholder (placeholderIdx == -1).
        if (placeholderIdx + 1 < opDesc.length())
        {
            L = new Label(opDesc.substring(placeholderIdx + 1));
            L.setForeground(LABEL_TXT_COLOR);
            optp.add(L);
            if (hasCB && ! readOnly)
            {
                controlsOpts.put(L, op);
                L.addMouseListener(this);  // Click label to toggle checkbox
            }
        }

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbl.setConstraints(optp, gbc);
        bp.add(optp);
    }

    /**
     * Based on this game option's type, present its intvalue either as
     * a numeric textfield, or a popup menu if min/max are near each other.
     * The maximum min/max distance which creates a popup is {@link #INTFIELD_POPUP_MAXRANGE}.
     * @param op A SOCGameOption with an integer value, that is,
     *           of type {@link SOCGameOption#OTYPE_INT OTYPE_INT}
     *           or {@link SOCGameOption#OTYPE_INTBOOL OTYPE_INTBOOL}
     * @return an IntTextField or {@link java.awt.Choice} (popup menu)
     */
    private Component initOption_int(SOCGameOption op)
    {
        // OTYPE_* - if a new type is added, update this method's javadoc.

        int optrange = op.maxIntValue - op.minIntValue;
        Component c;
        if ((optrange > INTFIELD_POPUP_MAXRANGE) || (optrange < 0))
        {
            // IntTextField with width based on number of digits in min/max .
            int amaxv = Math.abs(op.maxIntValue);
            int aminv = Math.abs(op.minIntValue);
            final int magn;
            if (amaxv > aminv)
                magn = amaxv;
            else
                magn = aminv;
            int twidth = 1 + (int) Math.ceil(Math.log10(magn));
            if (twidth < 3)
                twidth = 3;
            c = new IntTextField(op.getIntValue(), twidth);
            ((TextField) c).addTextListener(this);  // for op.ChangeListener and userChanged
        } else {
            Choice ch = new Choice();
            for (int i = op.minIntValue; i <= op.maxIntValue; ++i)
                ch.add(Integer.toString(i));

            int defaultIdx = op.getIntValue() - op.minIntValue;
            if (defaultIdx > 0)
                ch.select(defaultIdx);
            ch.addItemListener(this);  // for op.ChangeListener and userChanged
            c = ch;
        }
        return c;
    }

    /**
     * Create a popup menu for the choices of this enum.
     * @param op Game option, of type {@link SOCGameOption#OTYPE_ENUM OTYPE_ENUM}
     *           or {@link SOCGameOption#OTYPE_ENUMBOOL OTYPE_ENUMBOOL}
     */
    private Choice initOption_enum(SOCGameOption op)
    {
        Choice ch = new Choice();
        final String[] chs = op.enumVals;
        for (int i = 0; i < chs.length; ++i)
            ch.add(chs[i]);

        int defaultIdx = op.getIntValue() - 1;  // enum numbering is 1-based
        if (defaultIdx > 0)
            ch.select(defaultIdx);
        ch.addItemListener(this);  // for op.ChangeListener and userChanged
        return ch;
    }

    /**
     * Build UI for user preferences such as {@link SOCPlayerClient#PREF_SOUND_ON}
     * and {@link SOCPlayerInterface#PREF_SOUND_MUTE}. Fills {@link #localPrefs}.
     *<P>
     * Called from {@link #initInterface_Options(JPanel, GridBagLayout, GridBagConstraints)}.
     * @param bp  Add to this panel
     * @param gbl Use this layout
     * @param gbc Use these constraints
     * @since 1.2.00
     */
    private void initInterface_UserPrefs
        (final JPanel bp, final GridBagLayout gbl, final GridBagConstraints gbc)
    {
        // For current games we aren't playing in, don't show some prefs
        final boolean withPerGamePrefs = forNewGame || (pi != null);

        // thin <HR>-type spacer above prefs section

        JSeparator spacer = new JSeparator();
        spacer.setBackground(HEADER_LABEL_BG);
        gbl.setConstraints(spacer, gbc);
        bp.add(spacer);

        // reminder: same gbc widths/weights are used in initInterface_Opt1

        // PREF_SOUND_ON

        initInterface_Pref1
            (bp, gbl, gbc, null,
             strings.get("game.options.sound.all"),  // "Sound effects (All games)"
             true, false,
             SOCPlayerClient.GameAwtDisplay.getUserPreference(SOCPlayerClient.PREF_SOUND_ON, true), 0,
             new PrefCheckboxListener()
             {
                 public void stateChanged(boolean check)
                 {
                     SOCPlayerClient.GameAwtDisplay.putUserPreference
                         (SOCPlayerClient.PREF_SOUND_ON, check);
                 }
             });

        // Per-PI prefs:
        if (withPerGamePrefs)
        {
            // PREF_SOUND_MUTE

            boolean bval = (pi != null) ? pi.isSoundMuted() : false;
            localPrefs.put(SOCPlayerInterface.PREF_SOUND_MUTE, Boolean.valueOf(bval));
            initInterface_Pref1
                (bp, gbl, gbc, null,
                 strings.get("game.options.sound.mute_this"),  // "Sound: Mute this game"
                 true, false, bval, 0,
                 new PrefCheckboxListener()
                 {
                     public void stateChanged(boolean check)
                     {
                         if (pi != null)
                             pi.setSoundMuted(check);
                         else
                             localPrefs.put(SOCPlayerInterface.PREF_SOUND_MUTE, Boolean.valueOf(check));
                     }
                 });

            // PREF_BOT_TRADE_REJECT_SEC

            int ival = (pi != null)
                ? pi.getBotTradeRejectSec()
                : SOCPlayerClient.GameAwtDisplay.getUserPreference(SOCPlayerClient.PREF_BOT_TRADE_REJECT_SEC, -8);
            localPrefs.put(SOCPlayerClient.PREF_BOT_TRADE_REJECT_SEC, Integer.valueOf(ival));
            bval = (ival > 0);
            if (! bval)
                ival = -ival;
            initInterface_Pref1
                (bp, gbl, gbc, SOCPlayerClient.PREF_BOT_TRADE_REJECT_SEC,
                 strings.get("game.options.bot.auto_reject"),  // "Auto-reject bot trades after # seconds"
                 true, true, bval, ival, null);
        }
    }

    /**
     * Set up one preference row (desc label, checkbox and/or input box)
     * for {@link #initInterface_UserPrefs(JPanel, GridBagLayout, GridBagConstraints)}.
     * @param bp  Add to this panel
     * @param gbl Use this layout
     * @param gbc Use these constraints
     * @param key Pref key name to update in {@link #localPrefs} when changed,
     *     such as {@link SOCPlayerClient#PREF_BOT_TRADE_REJECT_SEC}, or {@code null}.
     *     If {@code hasBool} but not {@code hasInt}, will store {@link Boolean#TRUE} or {@code .FALSE} as key's value.
     *     If {@code hasInt} and can't parse text field contents, stores {@link Integer} 0 as key's value.
     *     If both bool and int, will store an {@code Integer} which is negative if checkbox is unchecked.
     * @param desc  Preference description text to show. If {@code hasInt}, must contain {@code "#"} placeholder.
     * @param hasBool  True if preference has a boolean value
     * @param hasInt   True if preference has an integer value
     * @param initBoolVal  Pref's initial boolean value, for checkbox; ignored unless {@code hasBool}
     * @param initIntVal   Pref's initial integer value, for input box; ignored unless {@code hasInt}
     * @param pcl  Callback when checkbox is checked/unchecked by clicking the box or its label, or {@code null}
     * @throws IllegalArgumentException if {@code hasInt} but {@code desc} doesn't contain {@code "#"},
     *     or if both {@code key} and {@code pcl} are {@code null}
     * @since 1.2.00
     */
    private void initInterface_Pref1
        (final JPanel bp, final GridBagLayout gbl, final GridBagConstraints gbc,
         final String key, final String desc, final boolean hasBool, final boolean hasInt,
         final boolean initBoolVal, final int initIntVal, final PrefCheckboxListener pcl)
        throws IllegalArgumentException
    {
        if ((key == null) && (pcl == null))
            throw new IllegalArgumentException("null key, pcl");

        // reminder: same gbc widths/weights are used in initInterface_Opt1

        final Checkbox cb;
        final IntTextField itf = (hasInt) ? new IntTextField(initIntVal, 3) : null;
        final MouseListener ml;
        if (hasBool)
        {
            cb = new Checkbox();
            cb.setState(initBoolVal);
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbl.setConstraints(cb, gbc);
            bp.add(cb);

            ml = new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    final boolean makeChecked = ! cb.getState();
                    cb.setState(makeChecked);
                    if (pcl != null)
                        pcl.stateChanged(makeChecked);
                    if (key != null)
                    {
                        if (hasInt)
                        {
                            int iv = 0;
                            try
                            {
                                iv = Integer.parseInt(itf.getText().trim());
                                if (! makeChecked)
                                    iv = -iv;
                            } catch (NumberFormatException nfe) {}

                            localPrefs.put(key, Integer.valueOf(iv));
                        } else {
                            localPrefs.put(key, (makeChecked) ? Boolean.TRUE : Boolean.FALSE);
                        }
                    }
                    e.consume();
                }
            };
        } else {
            cb = null;
            ml = null;
        }

        final int placeholderIdx;
        final Panel prefp;  // null or holds label with start of desc, int input field, label with rest of desc
        if (hasInt)
        {
            placeholderIdx = desc.indexOf('#');
            if (placeholderIdx == -1)
                throw new IllegalArgumentException("missing '#'");

            prefp = new Panel();  // with FlowLayout
            try
            {
                FlowLayout fl = (FlowLayout) (prefp.getLayout());
                fl.setAlignment(FlowLayout.LEFT);
                fl.setVgap(0);
                fl.setHgap(0);
            }
            catch (Exception fle) {}

        } else {
            placeholderIdx = -1;
            prefp = null;
        }

        // Any text to the left of placeholder in desc?
        if (placeholderIdx > 0)
        {
            Label L = new Label(desc.substring(0, placeholderIdx));
            L.setForeground(LABEL_TXT_COLOR);
            prefp.add(L);
            L.addMouseListener(ml);
        }

        if (hasInt)
        {
            prefp.add(itf);

            itf.addKeyListener(this);   // for ESC/ENTER

            if ((cb != null) || (key != null))
                itf.addTextListener(new TextListener()  // for value store or enable/disable
                {
                    public void textValueChanged(TextEvent arg0)
                    {
                        final String newText = itf.getText().trim();
                        final boolean notEmpty = (newText.length() > 0);

                        if (cb != null)
                        {
                            if (notEmpty != cb.getState())
                            {
                                cb.setState(notEmpty);
                                if (pcl != null)
                                    pcl.stateChanged(notEmpty);
                            }
                        }

                        if (key != null)
                        {
                            int iv = 0;
                            try
                            {
                                iv = Integer.parseInt(newText);
                                if ((cb != null) && ! cb.getState())
                                    iv = -iv;
                            } catch (NumberFormatException nfe) {}

                            localPrefs.put(key, Integer.valueOf(iv));
                        }
                    }
                });
        }

        // Any text to the right of placeholder?  Also creates
        // the text label if there is no placeholder.
        if (placeholderIdx + 1 < desc.length())
        {
            Label L = new Label(desc.substring(placeholderIdx + 1));
            L.setForeground(LABEL_TXT_COLOR);
            if (prefp != null)
            {
                prefp.add(L);
            } else {
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbl.setConstraints(L, gbc);
                bp.add(L);
            }
            L.addMouseListener(ml);
        }

        if (prefp != null)
        {
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbl.setConstraints(prefp, gbc);
            bp.add(prefp);
        }

        if (cb != null)
            cb.addItemListener(new ItemListener()
            {
                public void itemStateChanged(ItemEvent ie)
                {
                    pcl.stateChanged(ie.getStateChange() == ItemEvent.SELECTED);
                }
            });
    }

    /**
     * When the window is shown, request focus on game name textfield.
     * To make this window topmost, call {@code setVisible(true)} instead of {@link #requestFocus()}.
     */
    @Override
    public void setVisible(boolean b)
    {
        super.setVisible(b);
        if (b)
        {
            EventQueue.invokeLater(new Runnable()
            {
                public void run()
                {
                    toFront();  // needed on win32 at least
                    gameName.requestFocus();
                }
            });
        }
    }

    /**
     * React to button clicks.
     *<P>
     * Even in read-only mode for a current game, the "OK" button saves (persists)
     * this dialog's local preferences for use in future games.
     */
    public void actionPerformed(ActionEvent ae)
    {
        try
        {
            Object src = ae.getSource();
            if (src == create)
            {
                // Check options, ask client to set up and start a practice game
                clickCreate(true);
            }
            else if (src == cancel)
            {
                if (readOnly && (pi != null))
                    persistLocalPrefs();

                clickCancel();
            }
            else if (src == scenInfo)
            {
                clickScenarioInfo();
            }
            else if (src == scenDropdown)
            {
                if (opts == null)
                    return;
                SOCGameOption optSC = opts.get("SC");
                if (optSC == null)
                    return;

                Object scObj = scenDropdown.getSelectedItem();
                boolean wantsSet = (scObj instanceof SOCScenario);  // item 0 is "(none)" string, not a scenario
                optSC.setBoolValue(wantsSet);
                if (wantsSet)
                    optSC.setStringValue(((SOCScenario) scObj).key);
                else
                    optSC.setStringValue("");

                if (scenInfo != null)
                    scenInfo.setEnabled(wantsSet);

                boolean choiceSetCB = false;
                Checkbox cb = boolOptCheckboxes.get("SC");
                if ((cb != null) && (wantsSet != cb.getState()))
                {
                    cb.setState(wantsSet);
                    choiceSetCB = true;
                }

                fireUserChangedOptListeners(optSC, scenDropdown, wantsSet, choiceSetCB);
            }
        }
        catch(Throwable thr)
        {
            System.err.println("-- Error caught in AWT event thread: " + thr + " --");
            thr.printStackTrace();
            while (thr.getCause() != null)
            {
                thr = thr.getCause();
                System.err.println(" --> Cause: " + thr + " --");
                thr.printStackTrace();
            }
            System.err.println("-- Error stack trace end --");
            System.err.println();
        }

    }

    /**
     * The "Create" button was clicked; check fields, etc.
     * If creating new game, also calls {@link #persistLocalPrefs()}.
     */
    private void clickCreate(final boolean checkOptionsMinVers)
    {
        String gmName = gameName.getText().trim();
        final int L = gmName.length();
        if (L == 0)
        {
            return;  // Should not happen (button disabled by TextListener)
        }

        String errMsg = null;
        if (L > SOCGameList.GAME_NAME_MAX_LENGTH)
        {
            errMsg = SOCStatusMessage.MSG_SV_NEWGAME_NAME_TOO_LONG + SOCGameList.GAME_NAME_MAX_LENGTH;
                // "Please choose a shorter name; maximum length: "  TODO I18N
        }
        else if (! SOCMessage.isSingleLineAndSafe(gmName))
        {
            errMsg = SOCStatusMessage.MSG_SV_NEWGAME_NAME_REJECTED;
                // "This name is not permitted, please choose a different name."  TODO I18N
        }
        else if (SOCGameList.REGEX_ALL_DIGITS.matcher(gmName).matches())
        {
            errMsg = SOCStatusMessage.MSG_SV_NEWGAME_NAME_REJECTED_DIGITS;
                // "A name with only digits is not permitted, please add a letter."  TODO I18N
        }
        if (errMsg != null)
        {
            msgText.setText(errMsg);
            gameName.requestFocusInWindow();
            return;  // Not a valid game name
        }

        SOCPlayerClient cl = gameDisplay.getClient();

        /**
         * Is this game name already used?
         * Always check remote server for the requested game name.
         * Check practice game names only if creating another practice game.
         */
        boolean gameExists;
        if (forPractice)
            gameExists = (cl.getNet().practiceServer != null) && (-1 != cl.getNet().practiceServer.getGameState(gmName));
        else
            gameExists = false;
        if (cl.serverGames != null)
            gameExists = gameExists || cl.serverGames.isGame(gmName);
        if (gameExists)
        {
            NotifyDialog.createAndShow(gameDisplay, this, SOCStatusMessage.MSG_SV_NEWGAME_ALREADY_EXISTS, null, true);
            return;
        }

        if (gameDisplay.readValidNicknameAndPassword())
        {
            if (readOptsValuesFromControls(checkOptionsMinVers))
            {
                // All fields OK, ready to create a new game.
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));  // Immediate feedback in this frame
                persistLocalPrefs();
                gameDisplay.askStartGameWithOptions
                    (gmName, forPractice, opts, localPrefs);  // sets WAIT_CURSOR in main client frame
            } else {
                return;  // readOptsValues will put the err msg in dia's status line
            }
        } else {
            // Nickname field is also checked before this dialog is displayed,
            // so the user must have gone back and changed it.
            // Can't correct the problem from within this dialog, since the
            // nickname field (and hint message) is in SOCPlayerClient's panel.
            NotifyDialog.createAndShow(gameDisplay, this, strings.get("game.options.nickerror"), null, true);
            return;
        }

        dispose();
    }

    /**
     * The "Cancel" button or window's close button was clicked, or ESC was pressed; dismiss the frame.
     * Note: Button text is "OK" in read-only mode ({@link #readOnly}) for a current game.
     */
    private void clickCancel()
    {
        dispose();
    }

    /**
     * The "Scenario Info" button was clicked.
     * Reads the current scenario, if any, from {@link #scenDropdown}.
     * Calls {@link #showScenarioInfoDialog(SOCScenario, Map, int, SOCPlayerClient.GameAwtDisplay, Frame)}.
     * @since 2.0.00
     */
    private void clickScenarioInfo()
    {
        if (scenDropdown == null)
            return;  // should not happen, scenDropdown is created before scenInfo

        final Object scObj = scenDropdown.getSelectedItem();
        if ((scObj == null) || ! (scObj instanceof SOCScenario))
            return;  // "(none)" item is a String, not a scenario

        final SOCScenario scen = (SOCScenario) scObj;

        // find game's vp_winner
        int vpWinner = SOCGame.VP_WINNER_STANDARD;
        boolean vpKnown = false;
        if (opts != null)
        {
            SOCGameOption vp = opts.get("VP");
            if (vp.getBoolValue())
            {
                vpWinner = vp.getIntValue();
                vpKnown = true;
            }
        }
        if (forNewGame && (! vpKnown) && scen.scOpts.contains("VP="))
        {
            final Map<String, SOCGameOption> scenOpts = SOCGameOption.parseOptionsToMap(scen.scOpts);
            final SOCGameOption scOptVP = (scenOpts != null) ? scenOpts.get("VP") : null;
            if (scOptVP != null)
                vpWinner = scOptVP.getIntValue();
        }

        showScenarioInfoDialog(scen, null, vpWinner, gameDisplay, this);
    }

    /** Dismiss the frame, and clear client's {@link GameAwtDisplay#newGameOptsFrame}
     *  reference to null if it's to this frame.
     */
    @Override
    public void dispose()
    {
        if (this == gameDisplay.newGameOptsFrame)
            gameDisplay.newGameOptsFrame = null;

        super.dispose();
    }

    /**
     * When frame is closing, store any updated persistent local preferences
     * like {@link SOCPlayerClient#PREF_BOT_TRADE_REJECT_SEC}.
     * If {@link #pi} != null, update its settings too.
     *<P>
     * Prefs which update immediately when clicked, like {@link SOCPlayerClient#PREF_SOUND_ON},
     * aren't updated here.
     * @since 1.2.00
     */
    private void persistLocalPrefs()
    {
        String k = SOCPlayerClient.PREF_BOT_TRADE_REJECT_SEC;
        Object v = localPrefs.get(k);
        if ((v != null) && (v instanceof Integer))
        {
            int iv = ((Integer) v).intValue();
            if (pi != null)
                pi.setBotTradeRejectSec(iv);
            if (iv != 0)
                SOCPlayerClient.GameAwtDisplay.putUserPreference(k, iv);
        }
    }

    /**
     * Read option values from controls, as prep to request the new game.
     * If there is a problem (out of range, bad character in integer field, etc),
     * set {@link #msgText} and set focus on the field.
     * @param checkOptionsMinVers Warn the user if the options will require a
     *           minimum client version?  Won't do so if {@link #forPractice} is set,
     *           because this isn't a problem for local practice games.
     *           The warning is skipped if that minimum is an old version
     *           &lt;= {@link Version#versionNumberMaximumNoWarn()}.
     * @return true if all were read OK, false if a problem (such as NumberFormatException)
     */
    private boolean readOptsValuesFromControls(final boolean checkOptionsMinVers)
    {
        if (readOnly)
            return false;  // shouldn't be called in that case

        boolean allOK = true;
        for (Component ctrl : controlsOpts.keySet())
        {
            if (ctrl instanceof Label)
                continue;
            SOCGameOption op = controlsOpts.get(ctrl);

            if (op.key.equals("SC"))
            {
                // Special case: AWT event listeners have already set its value from controls
                if (! op.getBoolValue())
                    op.setStringValue("");
                continue;
            }

            // OTYPE_* - new option types may have new AWT control objects, or
            //           may use the same controls with different contents as these.

            if (ctrl instanceof Checkbox)
            {
                op.setBoolValue(((Checkbox)ctrl).getState());
            }
            else if (ctrl instanceof TextField)
            {
                String txt = ((TextField) ctrl).getText().trim();
                if ((op.optType == SOCGameOption.OTYPE_STR)
                    || (op.optType == SOCGameOption.OTYPE_STRHIDE))
                {
                    try
                    {
                        op.setStringValue(txt);
                    } catch (IllegalArgumentException ex)
                    {
                        allOK = false;
                        msgText.setText(strings.get("game.options.singleline"));  // only a single line of text allowed
                        ctrl.requestFocusInWindow();
                    }
                } else {
                    // OTYPE_INT, OTYPE_INTBOOL; defer setting until after all checkboxes have been read
                }
            }
            else if (ctrl instanceof Choice)
            {
                // this works with OTYPE_INT, OTYPE_INTBOOL, OTYPE_ENUM, OTYPE_ENUMBOOL
                int chIdx = ((Choice) ctrl).getSelectedIndex();  // 0 to n-1
                if (chIdx != -1)
                    op.setIntValue(chIdx + op.minIntValue);
                else
                    allOK = false;
            }

        }  // for(opts)

        // OTYPE_INT, OTYPE_INTBOOL: now that all checkboxes have been read,
        //   set int values and see if in range; ignore where bool is not set (checkbox not checked).
        //   Use 0 if blank (still checks if in range).
        for (Component ctrl : controlsOpts.keySet())
        {
            if (! (ctrl instanceof TextField))
                continue;

            SOCGameOption op = controlsOpts.get(ctrl);
            if (op.optType == SOCGameOption.OTYPE_INTBOOL)
            {
                if (! op.getBoolValue())
                    continue;
            }
            else if (op.optType != SOCGameOption.OTYPE_INT)
            {
                continue;
            }

            String txt = ((TextField) ctrl).getText().trim();
            try
            {
                int iv;
                if (txt.length() > 0)
                    iv = Integer.parseInt(txt);
                else
                    iv = 0;

                op.setIntValue(iv);
                if (iv != op.getIntValue())
                {
                    allOK = false;
                    msgText.setText
                        (strings.get("game.options.outofrange", op.minIntValue, op.maxIntValue));  // "out of range"
                    ctrl.requestFocusInWindow();
                }
            } catch (NumberFormatException ex)
            {
                allOK = false;
                msgText.setText(strings.get("game.options.onlydigits"));  // "please use only digits here"
                ctrl.requestFocusInWindow();
            }

        }  // for(opts)

        if (allOK && checkOptionsMinVers && ! forPractice)
        {
            int optsVers = SOCVersionedItem.itemsMinimumVersion(controlsOpts);
            if ((optsVers > -1) && (optsVers > Version.versionNumberMaximumNoWarn()))
            {
                allOK = false;
                new VersionConfirmDialog(this, optsVers).setVisible(true);
            }
        }

        return allOK;
    }

    /** Handle Enter or Esc key (KeyListener) */
    public void keyPressed(KeyEvent e)
    {
        if (e.isConsumed())
            return;

        try
        {
            switch (e.getKeyCode())
            {
            case KeyEvent.VK_ENTER:
                clickCreate(true);
                break;

            case KeyEvent.VK_CANCEL:
            case KeyEvent.VK_ESCAPE:
                clickCancel();
                break;
            }  // switch(e)
        }  // try
        catch(Throwable thr)
        {
            System.err.println("-- Error caught in AWT event thread: " + thr + " --");
            thr.printStackTrace();
            while (thr.getCause() != null)
            {
                thr = thr.getCause();
                System.err.println(" --> Cause: " + thr + " --");
                thr.printStackTrace();
            }
            System.err.println("-- Error stack trace end --");
            System.err.println();
        }
    }

    /** Stub required by KeyListener */
    public void keyReleased(KeyEvent arg0) { }

    /** Stub required by KeyListener */
    public void keyTyped(KeyEvent arg0) { }

    /**
     * When gamename contents change, enable/disable buttons as appropriate. (TextListener)
     * Also handles {@link SOCGameOption#OTYPE_INTBOOL} textfield/checkbox combos.
     * Also sets {@link SOCGameOption#userChanged}.
     * @param e textevent from {@link #gameName}, or from a TextField in {@link #controlsOpts}
     */
    public void textValueChanged(TextEvent e)
    {
        if (readOnly)
            return;
        Object srcObj = e.getSource();
        if (! (srcObj instanceof TextField))
            return;
        final String newText = ((TextField) srcObj).getText().trim();
        final boolean notEmpty = (newText.length() > 0);
        if (srcObj == gameName)
        {
            if (notEmpty != create.isEnabled())
                create.setEnabled(notEmpty);  // enable "create" btn only if game name filled in
        }
        else
        {
            // Check for a ChangeListener for OTYPE_STR and OTYPE_STRHIDE,
            // OTYPE_INT and OTYPE_INTBOOL.
            // if source is OTYPE_INTBOOL, check its checkbox vs notEmpty.
            SOCGameOption opt = controlsOpts.get(srcObj);
            if (opt == null)
                return;
            final String oldText = opt.getStringValue();
            boolean validChange = false;
            boolean otypeIsInt;
            int oldIntValue = 0;

            if ((opt.optType == SOCGameOption.OTYPE_STR)
                 || (opt.optType == SOCGameOption.OTYPE_STRHIDE))
            {
                otypeIsInt = false;
                try
                {
                    opt.setStringValue(newText);
                    validChange = true;
                } catch (IllegalArgumentException ex)
                { }
            } else {
                otypeIsInt = true;
                try   // OTYPE_INT, OTYPE_INTBOOL
                {
                    final int iv = Integer.parseInt(newText);
                    oldIntValue = opt.getIntValue();
                    opt.setIntValue(iv);  // ignored if outside min,max range
                    if (iv == opt.getIntValue())
                        validChange = true;
                } catch (NumberFormatException ex)
                { }
            }

            if (validChange && ! opt.userChanged)
                opt.userChanged = true;

            // If this string or int option also has a bool checkbox,
            // set or clear that based on string/int not empty.
            boolean cbSet = false;
            Checkbox cb = boolOptCheckboxes.get(opt.key);
            if ((cb != null) && (notEmpty != cb.getState()))
            {
                cb.setState(notEmpty);
                opt.setBoolValue(notEmpty);
                cbSet = true;
            }

            SOCGameOption.ChangeListener cl = opt.getChangeListener();
            if (cl == null)
                return;

            // If both bool and int fields are changed, update both before
            // calling fireOptionChangeListener.  Boolean is called before int.
            if (cbSet)
            {
                // ChangeListener for checkbox
                final Boolean newValue = (notEmpty) ? Boolean.TRUE : Boolean.FALSE;
                final Boolean oldValue = (notEmpty) ? Boolean.FALSE : Boolean.TRUE;
                fireOptionChangeListener(cl, opt, oldValue, newValue);
            }
            // ChangeListener for text field
            if (validChange)
            {
                if (otypeIsInt)
                    fireOptionChangeListener(cl, opt, new Integer(oldIntValue), new Integer(opt.getIntValue()));
                else
                    fireOptionChangeListener(cl, opt, oldText, newText);
            }
        }
    }

    /**
     * Called when a Choice or Checkbox value changes (ItemListener).
     * Used for these things:
     *<UL>
     * <LI>
     * Set {@link SOCGameOption#userChanged}
     * <LI>
     * Check Choices or Checkboxes to see if their game option has a {@link SOCGameOption.ChangeListener ChangeListener}.
     * <LI>
     * Set the checkbox when the popup-menu Choice value is changed for a
     * {@link SOCGameOption#OTYPE_INTBOOL} or {@link SOCGameOption#OTYPE_ENUMBOOL}.
     * <LI>
     * Update game option {@code "SC"} and the {@link #scenInfo} button when a scenario is picked
     * from {@link #scenDropdown}. Other scenario-related updates are handled by this method calling
     * {@link SOCGameOption.ChangeListener#valueChanged(SOCGameOption, Object, Object, Map)}.
     *</UL>
     * @param e itemevent from a Choice or Checkbox in {@link #controlsOpts}
     */
    public void itemStateChanged(ItemEvent e)
    {
        final Object ctrl = e.getSource();
        SOCGameOption opt = controlsOpts.get(ctrl);
        if (opt == null)
            return;

        boolean wasCBEvent = false, choiceSetCB = false;

        Checkbox cb = boolOptCheckboxes.get(opt.key);
        if ((cb != null) && (cb != ctrl))
        {
            // If the user picked a choice, also set the checkbox
            boolean wantsSet = true;  // any item sets it

            if (wantsSet != cb.getState())
            {
                cb.setState(wantsSet);
                choiceSetCB = true;
            }
        }
        else if (ctrl instanceof Checkbox)
        {
            wasCBEvent = true;
            choiceSetCB = (e.getStateChange() == ItemEvent.SELECTED);
        }

        fireUserChangedOptListeners(opt, ctrl, choiceSetCB, wasCBEvent);
    }

    /**
     * A game option's value widget was changed by the user.  If this game option has a
     * {@link SOCGameOption.ChangeListener}, call it with the appropriate old and new values.
     * Call to update {@code opt}'s value fields:
     *<UL>
     * <LI> If {@code changeBoolValue}, calls {@link SOCGameOption#setBoolValue(boolean) opt.setBoolValue(newBoolValue)}
     * <LI> If {@code ctrl} is a {@link Choice} or {@link JComboBox}, calls
     *      {@link SOCGameOption#setIntValue(int) opt.setIntValue}
     *      ({@link Choice#getSelectedIndex() ctrl.getSelectedIndex()})
     *</UL>
     * Calls {@link #fireOptionChangeListener(soc.game.SOCGameOption.ChangeListener, SOCGameOption, Object, Object)}
     * for the Option's boolean and/or int values.
     *
     * @param opt  Game option changed
     * @param ctrl  The {@link Checkbox} or {@link Choice} or {@link JComboBox} dropdown changed by the user
     * @param newBoolValue  New value to set for {@link SOCGameOption#getBoolValue() opt.getBoolValue()}
     * @param changeBoolValue True if the user changed the opt's boolean value, false if
     *     the opt's int or string value dropdown was changed but boolean wasn't.
     * @since 2.0.00
     */
    final private void fireUserChangedOptListeners
        (final SOCGameOption opt, final Object ctrl, final boolean newBoolValue, final boolean changeBoolValue)
    {
        if (! opt.userChanged)
            opt.userChanged = true;

        SOCGameOption.ChangeListener cl = opt.getChangeListener();
        if (cl == null)
            return;

        // If both bool and int fields are changed, update both before
        // calling fireOptionChangeListener.  Boolean is called before int.
        final boolean fireBooleanListener;
        final Object boolOldValue, boolNewValue;

        if (newBoolValue || changeBoolValue)
        {
            fireBooleanListener = true;
            boolNewValue = (newBoolValue) ? Boolean.TRUE : Boolean.FALSE;
            boolOldValue = (newBoolValue) ? Boolean.FALSE : Boolean.TRUE;
            opt.setBoolValue(newBoolValue);
        } else {
            fireBooleanListener = false;
            boolNewValue = null;
            boolOldValue = null;
        }

        if ((ctrl instanceof Choice) || (ctrl instanceof JComboBox))
        {
            int chIdx;
            if (ctrl instanceof Choice)
                chIdx = ((Choice) ctrl).getSelectedIndex();  // 0 to n-1
            else
                chIdx = ((JComboBox) ctrl).getSelectedIndex();

            if (chIdx != -1)
            {
                final int nv = chIdx + opt.minIntValue;
                Integer newValue = new Integer(nv);
                Integer oldValue = new Integer(opt.getIntValue());
                opt.setIntValue(nv);
                if (fireBooleanListener)
                    fireOptionChangeListener(cl, opt, boolOldValue, boolNewValue);
                fireOptionChangeListener(cl, opt, oldValue, newValue);
            }
        }
        else if (fireBooleanListener)
            fireOptionChangeListener(cl, opt, boolOldValue, boolNewValue);
    }

    /**
     * Handle firing a game option's ChangeListener, and refreshing related
     * gameopts' values on-screen if needed.
     * If <tt>oldValue</tt>.equals(<tt>newValue</tt>), nothing happens and
     * the ChangeListener is not called.
     * @param cl  The ChangeListener; must not be null
     * @param opt  The game option
     * @param oldValue  Old value, string or boxed primitive
     * @param newValue  New value, string or boxed primitive
     * @since 1.1.13
     */
    private void fireOptionChangeListener
        (SOCGameOption.ChangeListener cl, SOCGameOption opt, final Object oldValue, final Object newValue)
    {
        if (oldValue.equals(newValue))
            return;  // <--- Early return: Value didn't change ---

        try
        {
            cl.valueChanged(opt, oldValue, newValue, opts);
        } catch (Throwable thr) {
            System.err.println("-- Error caught in ChangeListener: " + thr.toString() + " --");
            thr.printStackTrace();
            while (thr.getCause() != null)
            {
                thr = thr.getCause();
                System.err.println(" --> Cause: " + thr + " --");
                thr.printStackTrace();
            }
            System.err.println("-- Error stack trace end --");
            System.err.println();
        }

        List<SOCGameOption> refresh = SOCGameOption.getAndClearRefreshList();
        if (refresh == null)
            return;  // <--- Early return: Nothing else changed ---

        // Refresh each one now, depending on type:
        if (optsControls == null)
            return;  // should only be null if readOnly, and thus no changes to values anyway
        for (int i = refresh.size() - 1; i >= 0; --i)
        {
            final SOCGameOption op = refresh.get(i);
            final Component opComp = optsControls.get(op.key);

            switch (op.optType)  // OTYPE_*
            {
            case SOCGameOption.OTYPE_BOOL:
                ((Checkbox) opComp).setState(op.getBoolValue());
                break;

            case SOCGameOption.OTYPE_INT:
            case SOCGameOption.OTYPE_INTBOOL:
                {
                    if (opComp instanceof TextField)
                        ((TextField) opComp).setText(Integer.toString(op.getIntValue()));
                    else
                        ((Choice) opComp).select(op.getIntValue() - op.minIntValue);
                    final boolean hasCheckbox = (op.optType == SOCGameOption.OTYPE_INTBOOL);
                    if (hasCheckbox)
                    {
                        Checkbox cb = boolOptCheckboxes.get(op.key);
                        if (cb != null)
                            cb.setState(op.getBoolValue());
                    }
                }
                break;

            case SOCGameOption.OTYPE_ENUM:
            case SOCGameOption.OTYPE_ENUMBOOL:
                {
                    ((Choice) opComp).select(op.getIntValue() - op.minIntValue);
                    final boolean hasCheckbox = (op.optType == SOCGameOption.OTYPE_ENUMBOOL);
                    if (hasCheckbox)
                    {
                        Checkbox cb = boolOptCheckboxes.get(op.key);
                        if (cb != null)
                            cb.setState(op.getBoolValue());
                    }
                }
                break;

            case SOCGameOption.OTYPE_STR:
            case SOCGameOption.OTYPE_STRHIDE:
                ((TextField) opComp).setText(op.getStringValue());
                break;

                // default: unknown, see above
            }
        }
    }

    /** when an option with a boolValue's label is clicked, toggle its checkbox */
    public void mouseClicked(MouseEvent e)
    {
        SOCGameOption opt = controlsOpts.get(e.getSource());
        if (opt == null)
            return;
        Checkbox cb = boolOptCheckboxes.get(opt.key);
        if (cb == null)
            return;

        final boolean becameChecked = ! cb.getState();
        cb.setState(becameChecked);
        opt.setBoolValue(becameChecked);
        if (! opt.userChanged)
            opt.userChanged = true;

        SOCGameOption.ChangeListener cl = opt.getChangeListener();
        if (cl == null)
            return;

        final Boolean newValue = (becameChecked) ? Boolean.TRUE : Boolean.FALSE;
        final Boolean oldValue = (becameChecked) ? Boolean.FALSE : Boolean.TRUE;
        fireOptionChangeListener(cl, opt, oldValue, newValue);
    }

    /** required stub for MouseListener */
    public void mouseEntered(MouseEvent e) {}

    /** required stub for MouseListener */
    public void mouseExited(MouseEvent e) {}

    /** required stub for MouseListener */
    public void mousePressed(MouseEvent e) {}

    /** required stub for MouseListener */
    public void mouseReleased(MouseEvent e) {}

    /**
     * Show a popup window with this game's scenario's description, special rules, and number of victory points to win.
     * Calls {@link EventQueue#invokeLater(Runnable)}.
     * @param ga  Game to display scenario info for; if game option {@code "SC"} missing or blank, does nothing.
     * @param cli     Player client interface, for {@link NotifyDialog} call
     * @param parent  Current game's player interface, or another Frame for our parent window,
     *                or null to look for {@code cli}'s Frame as parent
     * @since 2.0.00
     */
    public static void showScenarioInfoDialog
        (final SOCGame ga, final GameAwtDisplay cli, final Frame parent)
    {
        final String scKey = ga.getGameOptionStringValue("SC");
        if (scKey == null)
            return;

        SOCScenario sc = SOCScenario.getScenario(scKey);
        if (sc == null)
            return;

        showScenarioInfoDialog(sc, ga.getGameOptions(), ga.vp_winner, cli, parent);
    }

    /**
     * Show a popup window with this scenario's description, special rules, and number of victory points to win.
     * Calls {@link EventQueue#invokeLater(Runnable)}.
     * @param sc  A {@link SOCScenario}, or {@code null} to do nothing
     * @param gameOpts  All game options if current game, or null to extract from {@code sc}'s {@link SOCScenario#scOpts}
     * @param vpWinner  Number of victory points to win, or {@link SOCGame#VP_WINNER_STANDARD}.
     * @param cli     Player client interface, required for {@link AskDialog} constructor
     * @param parent  Current game's player interface, or another Frame for our parent window,
     *                or null to look for {@code cli}'s Frame as parent
     * @since 2.0.00
     */
    public static void showScenarioInfoDialog
        (final SOCScenario sc, Map<String, SOCGameOption> gameOpts, final int vpWinner,
         final GameAwtDisplay cli, final Frame parent)
    {
        if (sc == null)
            return;

        StringBuilder sb = new StringBuilder();
        sb.append(strings.get("game.options.scenario.label"));  // "Game Scenario:"
        sb.append(' ');
        sb.append(sc.getDesc());
        sb.append('\n');

        final String scLongDesc = sc.getLongDesc();
        if (scLongDesc != null)
        {
            sb.append('\n');
            sb.append(scLongDesc);
            sb.append('\n');
        }

        // Check game for any other _SC_ game opts in effect:

        final String scenOptName = "_" + sc.key;  // "_SC_CLVI"
        final String optDescScenPrefix = strings.get("game.options.scenario.optprefix");  // "Scenarios:"
        //      I18N note: showScenarioInfoDialog() assumes scenario game options
        //      all start with the text "Scenarios:". When localizing, be sure to
        //      keep a consistent prefix that showScenarioInfoDialog() knows to look for.

        if ((gameOpts == null) && (sc.scOpts != null))
            gameOpts = SOCGameOption.parseOptionsToMap(sc.scOpts);

        if (gameOpts != null)
        {
            for (SOCGameOption sgo : gameOpts.values())
            {
                if (sgo.key.equals(scenOptName))
                    continue;  // scenario's dedicated game option; we already showed its name from scDesc
                if (! sgo.key.startsWith("_SC_"))
                    continue;

                String optDesc = sgo.getDesc();
                if (optDesc.startsWith(optDescScenPrefix))
                    optDesc = optDesc.substring(optDescScenPrefix.length()).trim();
                sb.append("\n\u2022 ");  // bullet point before option text
                sb.append(optDesc);
            }
        }

        if (vpWinner != SOCGame.VP_WINNER_STANDARD)
        {
            sb.append("\n\u2022 ");
            sb.append(strings.get("game.options.scenario.vp"));  // "Victory Points to win:"
            sb.append(' ');
            sb.append(vpWinner);
        }

        final String scenStr = sb.toString();
        NotifyDialog.createAndShow(cli, parent, scenStr, null, true);
    }


    /**
     * A textfield that accepts only nonnegative-integer characters.
     * @author Jeremy D Monin <jeremy@nand.net>
     */
    public class IntTextField extends TextField implements KeyListener
    {
        IntTextField(int initVal, int width)
        {
            super(Integer.toString(initVal), width);
            addKeyListener(this);
        }

        /**
         * Parse the value of this textfield
         * @return value, or 0 if can't parse it
         */
        public int getIntValue()
        {
            String txt = getText().trim();
            if (txt.length() == 0)
                return 0;
            try
            {
                return Integer.parseInt(txt);
            }
            catch (NumberFormatException e)
            {
                return 0;
            }
        }

        /** stub for KeyListener */
        public void keyPressed(KeyEvent e) {}

        /** stub for KeyListener */
        public void keyReleased(KeyEvent e) {}

        /** reject entered characters which aren't digits */
        public void keyTyped(KeyEvent e)
        {
            // TODO this is not always rejecting non-digits

            switch (e.getKeyCode())
            {
            case KeyEvent.VK_ENTER:
                clickCreate(true);
                break;

            case KeyEvent.VK_CANCEL:
            case KeyEvent.VK_ESCAPE:
                clickCancel();
                break;

            default:
                {
                    final char c = e.getKeyChar();
                    switch (c)
                    {
                    case KeyEvent.CHAR_UNDEFINED:  // ctrl characters, arrows, etc
                    case (char) 8:    // backspace
                    case (char) 127:  // delete
                        return;  // don't consume

                    default:
                        if (! Character.isDigit(c))
                            e.consume();  // ignore non-digits
                    }
                }
            }  // switch(e)
        }

    }  // public inner class IntTextField


    /**
     * Callback for when a user preference checkbox is checked/unchecked by clicking that box or its label.
     * @see NewGameOptionsFrame#initInterface_Pref1(JPanel, GridBagLayout, GridBagConstraints, String, String, boolean, boolean, boolean, int, PrefCheckboxListener)
     * @since 1.2.00
     */
    private static interface PrefCheckboxListener
    {
        /**
         * Callback for when checkbox becomes checked or unchecked.
         * Also called when checkbox's label is clicked.
         * @param check New value of checkbox: True if becoming checked
         */
        public void stateChanged(final boolean check);
    }


    /**
     * This is the modal dialog to ask user if these options' required
     * minimum client version is OK.
     *
     * @author Jeremy D Monin <jeremy@nand.net>
     */
    private class VersionConfirmDialog extends AskDialog
    {
        /**
         * Creates a new VersionConfirmDialog.
         *
         * @param ngof  Parent options-frame, which contains these options
         * @param minVers  Minimum required version for these options
         */
        public VersionConfirmDialog(NewGameOptionsFrame ngof, int minVers)
        {
            super(gameDisplay, ngof, strings.get("game.options.verconfirm.title"),
                strings.get("game.options.verconfirm.prompt", Version.version(minVers)),
                strings.get("game.options.verconfirm.create"),
                strings.get("game.options.verconfirm.change"), true, false);
        }

        /**
         * React to the Create button.
         */
        @Override
        public void button1Chosen()
        {
            clickCreate(false);
        }

        /**
         * React to the Change button.
         */
        @Override
        public void button2Chosen()
        {
            dispose();
        }

        /**
         * React to the dialog window closed by user, or Esc pressed. (same as Change button)
         */
        @Override
        public void windowCloseChosen()
        {
            button2Chosen();
        }

    }  // private inner class VersionConfirmDialog


}  // public class NewGameOptionsFrame
