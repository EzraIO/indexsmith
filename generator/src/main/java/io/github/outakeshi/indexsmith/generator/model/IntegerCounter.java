package io.github.outakeshi.indexsmith.generator.model;

import lombok.NoArgsConstructor;

/**
 */
@NoArgsConstructor
public class IntegerCounter {


    private int count = 0;

    public int getCount() {
        return count;
    }

    public int addAndGet() {
        return ++count;
    }

    public int getAndAdd() {
        return count++;
    }

}
