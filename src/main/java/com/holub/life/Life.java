package com.holub.life;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import com.holub.ui.MenuSite;

/*******************************************************************
 * An implemenation of Conway's Game of Life.
 *
 * @include /etc/license.txt
 */

public final class Life extends JFrame
{	
	private final Universe universe;
	private final StatisticsPanel statisticsPanel;
	private boolean statisticsSubscribed;

	public static void main( String[] arguments )
	{	SwingUtilities.invokeLater(Life::new);
	}

	private Life()
	{	super( "The Game of Life. "
					+"(c)2003 Allen I. Holub <http://www.holub.com>");

		// Must establish the MenuSite very early in case
		// a subcomponent puts menus on it.
		MenuSite.establish( this );		//{=life.java.establish}

		setDefaultCloseOperation	( EXIT_ON_CLOSE 		);
		getContentPane().setLayout	( new BorderLayout()	);
		universe = Universe.instance();
		getContentPane().add( universe, BorderLayout.CENTER); //{=life.java.install}
		JPanel controls = new JPanel();
		JButton undo = new JButton("Undo");
		undo.addActionListener(event -> universe.undo());
		controls.add(undo);
		JButton redo = new JButton("Redo");
		redo.addActionListener(event -> universe.redo());
		controls.add(redo);
		getContentPane().add( controls, BorderLayout.NORTH );
		statisticsPanel = new StatisticsPanel();
		getContentPane().add( statisticsPanel, BorderLayout.SOUTH );
		universe.addStatisticsListener(statisticsPanel);
		statisticsSubscribed = true;
		AutoStopMenu.install( Clock.instance(), universe );

		statisticsPanel.addHierarchyListener(event -> {
			if ((event.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0
					&& !statisticsPanel.isDisplayable()) {
				unsubscribeStatisticsPanel();
			}
		});
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				unsubscribeStatisticsPanel();
			}
		});

		pack();
		setVisible( true );
	}

	private void unsubscribeStatisticsPanel()
	{	if( statisticsSubscribed )
		{	universe.removeStatisticsListener(statisticsPanel);
			statisticsSubscribed = false;
		}
	}
}
