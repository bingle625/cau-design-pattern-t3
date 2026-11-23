package com.holub.life;

import java.awt.*;
import java.util.Objects;
import javax.swing.*;
import com.holub.life.Cell;
import com.holub.life.Storable;
import com.holub.life.Direction;
import com.holub.life.Neighborhood;
import com.holub.life.Universe;

/*** ****************************************************************
 * The Resident class implements a single cell---a "resident" of a
 * block.
 *
 * @include /etc/license.txt
 */

public final class Resident implements Cell
{
	/** 상태가 없으므로 View를 고르기 전까지 모든 세포가 공유한다. */
	private static final CellRenderer DEFAULT_RENDERER = new BasicRenderer();

	private boolean amAlive 	= false;
	private boolean willBeAlive	= false;

	/** 이 세포가 살아남은 세대 수. 죽었으면 0, 방금 나타났으면
	 *  (탄생, 클릭, 불러오기) 1이다.
	 */
	private int age = 0;
	private LifeRule rule;
	private CellRenderer renderer = DEFAULT_RENDERER;

	public Resident()
	{
		this(new ConwayRule());
	}

	public Resident(LifeRule rule)
	{
		setRule(rule);
	}

	void setRule(LifeRule rule)
	{
		this.rule = Objects.requireNonNull(rule);
	}

	void setRenderer(CellRenderer renderer)
	{
		this.renderer = Objects.requireNonNull(renderer);
	}

	private boolean isStable(){return amAlive == willBeAlive; }

	/** figure the next state.
	 *  @return true if the cell is not stable (will change state on the
	 *  next transition().
	 */
	public boolean figureNextState(
							Cell north, 	Cell south,
							Cell east, 		Cell west,
							Cell northeast, Cell northwest,
							Cell southeast, Cell southwest )
	{
		verify( north, 		"north"		);
		verify( south, 		"south"		);
		verify( east, 		"east"		);
		verify( west, 		"west"		);
		verify( northeast,	"northeast"	);
		verify( northwest,	"northwest" );
		verify( southeast,	"southeast" );
		verify( southwest,	"southwest" );

		int neighbors = 0;

		if( north.	  isAlive()) ++neighbors;
		if( south.	  isAlive()) ++neighbors;
		if( east. 	  isAlive()) ++neighbors;
		if( west. 	  isAlive()) ++neighbors;
		if( northeast.isAlive()) ++neighbors;
		if( northwest.isAlive()) ++neighbors;
		if( southeast.isAlive()) ++neighbors;
		if( southwest.isAlive()) ++neighbors;

		willBeAlive = rule.nextAlive(amAlive, neighbors);
		return !isStable();
	}

	private void verify( Cell c, String direction )
	{	assert (c instanceof Resident) || (c == Cell.DUMMY)
				: "incorrect type for " + direction +  ": " +
				   c.getClass().getName();
	}

	/** This cell is monetary, so it's at every edge of itself. It's
	 *  an internal error for any position except for (0,0) to be
	 *  requsted since the width is 1.
	 */
	public Cell	edge(int row, int column)
	{	assert row==0 && column==0;
		return this;
	}

	public boolean transition()
	{	boolean changed = !isStable();
		age = !willBeAlive ? 0 : amAlive ? age + 1 : 1;
		amAlive = willBeAlive;
		return changed;
	}

	public void redraw(Graphics g, Rectangle here, boolean drawAll)
	{	renderer.draw(g, here, amAlive, age);
	}

	public void userClicked(Point here, Rectangle surface)
	{	amAlive = !amAlive;
		age = amAlive ? 1 : 0;
	}

	public void	   clear()			{amAlive = willBeAlive = false; age = 0; }
	int			   age()			{return age;					}
	public boolean isAlive()		{return amAlive;			    }
	public Cell    create()
	{	Resident copy = new Resident(rule);
		copy.renderer = renderer;
		return copy;
	}
	public int 	   widthInCells()	{return 1;}

	public int countAlive()
	{	return amAlive ? 1 : 0;
	}

	public Direction isDisruptiveTo()
	{	return isStable() ? Direction.NONE : Direction.ALL ;
	}

	public boolean transfer(Storable blob,Point upperLeft,boolean doLoad)
	{
		Memento memento = (Memento)blob;
		if( doLoad )
		{	amAlive = willBeAlive = memento.isAlive(upperLeft);
			age = amAlive ? 1 : 0;		// memento에는 이력이 없다
			if( amAlive )
				return true;
		}
		else if( amAlive )  					// store only live cells
			memento.markAsAlive( upperLeft );

		return false;
	}

	/** Mementos must be created by Neighborhood objects. Throw an
	 *  exception if anybody tries to do it here.
	 */
	public Storable createMemento()
	{	throw new UnsupportedOperationException(
					"May not create memento of a unitary cell");
	}
}
