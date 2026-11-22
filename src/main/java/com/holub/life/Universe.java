package com.holub.life;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import com.holub.io.Files;
import com.holub.tools.Publisher;
import com.holub.ui.MenuSite;

import com.holub.life.Cell;
import com.holub.life.Storable;
import com.holub.life.Clock;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;

/**
 * The Universe is a mediator that sits between the Swing
 * event model and the Life classes. It is also a singleton,
 * accessed via Universe.instance(). It handles all
 * Swing events and translates them into requests to the
 * outermost Neighborhood. It also creates the Composite
 * Neighborhood.
 *
 * @include /etc/license.txt
 */

public class Universe extends JPanel
{	private final Neighborhood outermostCell;
	private final Map<String, LifeRule> rules = new LinkedHashMap<>();
	private LifeRule selectedRule = new ConwayRule();
	private static final String RULE_MENU = "Rule";
	private static	final Universe 	theInstance = new Universe();
	private final Publisher statisticsPublisher = new Publisher();
	private final CommandHistory history = new CommandHistory();

	/** The default height and width of a Neighborhood in cells.
	 *  If it's too big, you'll run too slowly because
	 *  you have to update the entire block as a unit, so there's more
	 *  to do. If it's too small, you have too many blocks to check.
	 *  I've found that 8 is a good compromise.
	 */
	private static final int  DEFAULT_GRID_SIZE = 8;

	/** The size of the smallest "atomic" cell---a Resident object.
	 *  This size is extrinsic to a Resident (It's passed into the
	 *  Resident's "draw yourself" method.
	 */
	private static final int  DEFAULT_CELL_SIZE = 8;

	// The constructor is private so that the universe can be created
	// only by an outer-class method [Neighborhood.createUniverse()].

	private Universe()
	{	// Create the nested Cells that comprise the "universe." A bug
		// in the current implementation causes the program to fail
		// miserably if the overall size of the grid is too big to fit
		// on the screen.

		outermostCell = new Neighborhood
						(	DEFAULT_GRID_SIZE,
							new Neighborhood
							(	DEFAULT_GRID_SIZE,
								new Resident(selectedRule)
							)
						);

		final Dimension PREFERRED_SIZE =
						new Dimension
						(  outermostCell.widthInCells() * DEFAULT_CELL_SIZE,
						   outermostCell.widthInCells() * DEFAULT_CELL_SIZE
						);

		addComponentListener
		(	new ComponentAdapter()
			{	public void componentResized(ComponentEvent e)
				{
					// Make sure that the cells fit evenly into the
					// total grid size so that each cell will be the
					// same size. For example, in a 64x64 grid, the
					// total size must be an even multiple of 63.

					Rectangle bounds = getBounds();
					bounds.height /= outermostCell.widthInCells();
					bounds.height *= outermostCell.widthInCells();
					bounds.width  =  bounds.height;
					setBounds( bounds );
				}
			}
		);

		setBackground	( Color.white	 );
		setPreferredSize( PREFERRED_SIZE );
		setMaximumSize	( PREFERRED_SIZE );
		setMinimumSize	( PREFERRED_SIZE );
		setOpaque		( true			 );

		addMouseListener					//{=Universe.mouse}
		(	new MouseAdapter()
			{	public void mousePressed(MouseEvent e)
				{	Rectangle bounds = getBounds();
					bounds.x = 0;
					bounds.y = 0;
					synchronized( history )
					{	outermostCell.userClicked(e.getPoint(),bounds);
						history.clear();
						repaint();
						publishStatistics();
					}
				}
			}
		);

		MenuSite.addLine( this, "Grid", "Clear",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	synchronized( history )
					{	outermostCell.clear();
						history.clear();
						repaint();
						publishStatistics();
					}
				}
			}
		);

		MenuSite.addLine			// {=Universe.load.setup}
		(	this, "Grid", "Load",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	doLoad();
				}
			}
		);

		MenuSite.addLine
		(	this, "Grid", "Store",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	doStore();
				}
			}
		);

		MenuSite.addLine
		(	this, "Grid", "Exit",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
		        {	System.exit(0);
		        }
			}
		);

		Clock.instance().addClockListener(this::advanceGeneration); //{=Universe.clock.subscribe}

		rules.put("Conway", selectedRule);
		rules.put("HighLife", new HighLifeRule());
		rules.put("Seeds", new SeedsRule());
		rules.forEach((name, rule) ->
			MenuSite.addLine(this, RULE_MENU, name, event -> selectRule(rule)));
		updateRuleMenu();
	}

	private void selectRule(LifeRule rule)
	{
		synchronized( history )
		{
			if( selectedRule == rule )
				return;
			outermostCell.changeRule(rule);
			selectedRule = rule;
		}
		updateRuleMenu();
	}

	private void updateRuleMenu()
	{
		rules.forEach((name, rule) -> {
			JMenuItem item = MenuSite.getMyMenuItem(this, RULE_MENU, name);
			item.setText(rule == selectedRule ? "✓ " + name : name);
		});
	}

	private void advanceGeneration()
	{
		synchronized( history )
		{
			Command command = new AdvanceGenerationCommand(outermostCell, Universe.this::refreshNow);
			history.execute(command);
			publishStatistics();
		}
	}

	/** Singleton Accessor. The Universe object itself is manufactured
	 *  in Neighborhood.createUniverse()
	 */

	public static Universe instance()
	{	return theInstance;
	}

	public void addStatisticsListener(StatisticsListener listener)
	{	statisticsPublisher.subscribe(listener);
	}

	public void removeStatisticsListener(StatisticsListener listener)
	{	statisticsPublisher.cancelSubscription(listener);
	}

	void publishStatistics()
	{	synchronized( history )
		{	GameStatistics statistics = new GameStatistics(outermostCell.countAlive());
			statisticsPublisher.publish(subscriber ->
				((StatisticsListener) subscriber).statisticsChanged(statistics));
		}
	}

	int generationHistorySize()
	{	return history.size();
	}


	public boolean undo()
	{	Clock.instance().stopAndWait();
		synchronized( history )
		{	if( !history.undo() )
				return false;
			repaint();
			publishStatistics();
			return true;
		}
	}

	public boolean redo()
	{	Clock.instance().stopAndWait();
		synchronized( history )
		{	if( !history.redo() )
				return false;
			repaint();
			publishStatistics();
			return true;
		}
	}

	private void doLoad()
	{	boolean loaded = false;
		try
		{
			Storable memento;
			try( FileInputStream in = new FileInputStream(
					Files.userSelected(".",".life","Life File","Load")) )
			{	Clock.instance().stop();
				// Prepare the memento on an empty board to leave the current board untouched.
				memento = outermostCell.create().createMemento();
				memento.load( in );
			}

			synchronized( history )
			{	outermostCell.clear();
				outermostCell.transfer( memento, new Point(0,0), Cell.LOAD );
				history.clear();
				loaded = true;
			}
		}
		catch( IOException theException )
		{	JOptionPane.showMessageDialog( null, "Read Failed!",
					"The Game of Life", JOptionPane.ERROR_MESSAGE);
		}
		repaint();
		if( loaded )
			publishStatistics();
	}

	private void doStore()
	{	try
		{
			FileOutputStream out = new FileOutputStream(
				  Files.userSelected(".",".life","Life File","Write"));

			Clock.instance().stop();		// stop the game

			Storable memento;
			synchronized( history )
			{	memento = outermostCell.createMemento();
				outermostCell.transfer( memento, new Point(0,0), Cell.STORE );
			}
			memento.flush(out);

			out.close();
		}
		catch( IOException theException )
		{	JOptionPane.showMessageDialog( null, "Write Failed!",
					"The Game of Life", JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Override paint to ask the outermost Neighborhood
	 *  (and any subcells) to draw themselves recursively.
	 *  All knowledge of screen size is also encapsulated.
	 *  (The size is passed into the outermost <code>Cell</code>.)
	 */

	public void paint(Graphics g)
	{
		Rectangle panelBounds = getBounds();
		Rectangle clipBounds  = g.getClipBounds();

		// The panel bounds is relative to the upper-left
		// corner of the screen. Pretend that it's at (0,0)
		panelBounds.x = 0;
		panelBounds.y = 0;
		synchronized( history )
		{	outermostCell.redraw(g, panelBounds, true);	//{=Universe.redraw1}
		}
	}

	/** Force a screen refresh by queing a request on
	 *  the Swing event queue. This is an example of the
	 *  Active Object pattern (not covered by the Gang of Four).
	 *  This method is called on every clock tick. Note that
	 *  the redraw() method on a given <code>Cell</code>
	 *  does nothing if the <code>Cell</code> doesn't
	 *  have to be refreshed.
	 */

	private void refreshNow()
	{	SwingUtilities.invokeLater
		(	new Runnable()
			{	public void run()
				{	Graphics g = getGraphics();
					if( g == null )		// Universe not displayable
						return;
					try
					{
						Rectangle panelBounds = getBounds();
						panelBounds.x = 0;
						panelBounds.y = 0;
						synchronized( history )
						{	outermostCell.redraw(g, panelBounds, false); //{=Universe.redraw2}
						}
					}
					finally
					{	g.dispose();
					}
				}
			}
		);
	}
}
