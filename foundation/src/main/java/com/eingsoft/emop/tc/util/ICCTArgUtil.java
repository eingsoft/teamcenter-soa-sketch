package com.eingsoft.emop.tc.util;

import com.teamcenter.services.internal.strong.core._2011_06.ICT;
import com.teamcenter.services.internal.strong.core._2011_06.ICT.Arg;

public class ICCTArgUtil {

    public static Arg createArg(String val) {
        ICT.Arg arg = new ICT.Arg();
        arg.val = val;
        return arg;
    }

    public static Arg createStructure(String val) {
        ICT.Structure structure = new ICT.Structure();
        ICT.Arg[] struArgs = new ICT.Arg[2];
        for (int i = 0; i < struArgs.length; i++) {
            struArgs[i] = new ICT.Arg();
        }
        struArgs[0].val = "true";
        struArgs[1].val = val;
        structure.args = struArgs;

        ICT.Arg arg = new ICT.Arg();
        arg.val = "";
        arg.structure = new ICT.Structure[] {structure};
        return arg;
    }

    public static Arg createEntry(String... vals) {
        ICT.Array array = new ICT.Array();
        ICT.Entry[] entries = new ICT.Entry[vals.length];
        for (int i = 0; i < vals.length; i++) {
            ICT.Entry entry = new ICT.Entry();
            entries[i] = entry;
            entry.val = vals[i];
        }
        array.entries = entries;
        ICT.Arg arg = new ICT.Arg();
        arg.val = "";
        arg.array = new ICT.Array[] {array};
        return arg;
    }
}
