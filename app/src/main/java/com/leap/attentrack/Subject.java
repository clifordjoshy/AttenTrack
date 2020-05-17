package com.leap.attentrack;

import java.io.Serializable;
import java.util.LinkedList;

class Subject implements Serializable {
    String name;
    int color;
    LinkedList<Integer>[] slots = new LinkedList[7];
    int attendance = 100, missable, missed = 0, total;

    static int req_percentage;
    static LinkedList<String[]> session_encoder;

    void cancel_session() {
        if (total == 1)
            return;
        total -= 1;
        attendance = (total - missed) * 100 / total;
        missable = (attendance - req_percentage) * total / 100;
    }

    void missed_session() {
        if (total == 0)
            return;
        missed += 1;
        missable -= 1;
        attendance = (total - missed) * 100 / total;
    }

    void unmiss_session() {
        if (total == 0)
            return;
        missed -= 1;
        missable += 1;
        attendance = (total - missed) * 100 / total;
    }

    void add_session() {
        total += 1;
        attendance = (total - missed) * 100 / total;
        missable = (attendance - req_percentage) * total / 100;
    }
}
