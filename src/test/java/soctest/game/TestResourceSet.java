/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2017 Ruud Poutsma <rtimon@gmail.com>
 * Portions of this file Copyright (C) 2017 Jeremy D Monin <jeremy@nand.net>
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
 *
 * The maintainer of this program can be reached at jsettlers@nand.net
 **/

package soctest.game;

import org.junit.Test;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.proto.Data;

import static org.junit.Assert.*;

public class TestResourceSet
{
    private static SOCResourceSet onePerType()
    {
        return new SOCResourceSet(1,1,1,1,1,0);
    }

    @Test
    public void total_test()
    {
        SOCResourceSet rs = onePerType();
        assertEquals(5, rs.getTotal());
    }

    @Test
    public void removeOneResource_removesOneResource()
    {
        SOCResourceSet rs = onePerType();
        rs.subtract(1, Data.ResourceType.ORE_VALUE);
        assertEquals(4, rs.getTotal());
    }

    @Test
    public void removeTwoResources_doesNotThrowException()
    {
        SOCResourceSet rs = onePerType();
        rs.subtract(2, Data.ResourceType.CLAY_VALUE);
        assertEquals(3, rs.getTotal());
        assertEquals(0, rs.getAmount(Data.ResourceType.CLAY_VALUE));
        assertEquals(-1, rs.getAmount(Data.ResourceType.UNKNOWN_VALUE));
    }

    @Test
    public void removeAll_yieldsEmptyResourceSet()
    {
        SOCResourceSet rs = onePerType();
        rs.subtract(1, Data.ResourceType.CLAY_VALUE);
        rs.subtract(1, Data.ResourceType.WHEAT_VALUE);
        rs.subtract(1, Data.ResourceType.WOOD_VALUE);
        rs.subtract(1, Data.ResourceType.ORE_VALUE);
        rs.subtract(1, Data.ResourceType.SHEEP_VALUE);
        assertEquals(0, rs.getTotal());
    }

    @Test
    public void removeAll_yieldsEmptyResourceSet2()
    {
        SOCResourceSet rs1 = onePerType();
        SOCResourceSet rs2 = onePerType();
        rs1.subtract(rs2);
        assertEquals(0, rs1.getTotal());
    }

    @Test
    public void clear_yieldsEmptyResourceSet()
    {
        SOCResourceSet rs = onePerType();
        rs.clear();
        assertEquals(0, rs.getTotal());
    }

    @Test
    public void onePerType_hasOnePerType()
    {
        SOCResourceSet rs = onePerType();
        assertTrue(rs.contains(Data.ResourceType.CLAY_VALUE));
        assertTrue(rs.contains(Data.ResourceType.WHEAT_VALUE));
        assertTrue(rs.contains(Data.ResourceType.WOOD_VALUE));
        assertTrue(rs.contains(Data.ResourceType.ORE_VALUE));
        assertTrue(rs.contains(Data.ResourceType.SHEEP_VALUE));
    }

    @Test
    public void onePerType_hasOnePerType2()
    {
        SOCResourceSet rs = onePerType();
        assertEquals(1, rs.getAmount(Data.ResourceType.CLAY_VALUE));
        assertEquals(1, rs.getAmount(Data.ResourceType.ORE_VALUE));
        assertEquals(1, rs.getAmount(Data.ResourceType.SHEEP_VALUE));
        assertEquals(1, rs.getAmount(Data.ResourceType.WOOD_VALUE));
        assertEquals(1, rs.getAmount(Data.ResourceType.WHEAT_VALUE));
    }

    @Test
    public void onePerType_typesAreKnown()
    {
        SOCResourceSet rs = onePerType();
        assertEquals(5, rs.getResourceTypeCount());
    }

    @Test
    public void addResourceSet_addsResourceSet()
    {
        SOCResourceSet rs1 = onePerType();
        SOCResourceSet rs2 = onePerType();
        rs1.add(rs2);
        assertEquals(2, rs1.getAmount(Data.ResourceType.CLAY_VALUE));
        assertEquals(2, rs1.getAmount(Data.ResourceType.WHEAT_VALUE));
        assertEquals(2, rs1.getAmount(Data.ResourceType.ORE_VALUE));
        assertEquals(2, rs1.getAmount(Data.ResourceType.WOOD_VALUE));
        assertEquals(2, rs1.getAmount(Data.ResourceType.SHEEP_VALUE));
        assertTrue(rs1.contains(Data.ResourceType.WHEAT_VALUE));
    }
    @Test
    public void clone_isContained()
    {
        SOCResourceSet rs1 = onePerType();
        SOCResourceSet rs2 = onePerType();
        assertTrue(rs1.contains(rs2));
    }

    @Test
    public void almostClone_isNotContained()
    {
        SOCResourceSet rs1 = onePerType();
        SOCResourceSet rs2 = onePerType();
        rs2.subtract(1, Data.ResourceType.CLAY_VALUE);
        assertTrue(rs1.contains(rs2));
        assertFalse(rs2.contains(rs1));
    }

    @Test
    public void clone_isEqual()
    {
        SOCResourceSet rs1 = onePerType();
        SOCResourceSet rs2 = onePerType();
        assertTrue(rs1.equals(rs2));
        assertTrue(rs2.equals(rs1));
    }

    @Test
    public void copyConstructor_ConstructsACopy()
    {
        SOCResourceSet all = onePerType();
        SOCResourceSet copy = new SOCResourceSet(all);
        assertEquals(all, copy);
    }
}
