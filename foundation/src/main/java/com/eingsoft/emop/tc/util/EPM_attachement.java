package com.eingsoft.emop.tc.util;

/**
 * EPM attachment type
 *
 */
public enum EPM_attachement {
    target(1), reference(3), signoff(4), release_status(5), comment(6), instruction(7), interprocess(8),
    project_task(9);

    private int value;

    private EPM_attachement(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
