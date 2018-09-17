package us.hebi.matlab.io.types;

import java.io.IOException;

import static us.hebi.matlab.common.util.Preconditions.*;

/**
 * Cell array implementation.
 *
 * Note that we don't need to check indices as the array access already
 * takes care of that, i.e., throws an out of bounds exception.
 *
 * @author Florian Enner < florian @ hebirobotics.com >
 * @since 07 Sep 2018
 */
public class AbstractCell extends AbstractCellBase {

    protected AbstractCell(int[] dims, boolean isGlobal, Array[] values) {
        super(dims, isGlobal);
        checkArgument(values.length == getNumElements(), "invalid length");
        this.contents = values;
    }

    @Override
    @SuppressWarnings("unchecked") // simplifies casting
    public <T extends Array> T get(int index) {
        return (T) contents[index];
    }

    @Override
    public Cell set(int index, Array value) {
        contents[index] = value;
        return this;
    }


    @Override
    public void close() throws IOException {
        for (Array array : contents) {
            array.close();
        }
    }

    protected final Array[] contents;

}
