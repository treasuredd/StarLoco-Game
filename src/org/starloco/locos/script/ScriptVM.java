package org.starloco.locos.script;

import org.classdump.luna.ByteString;
import org.classdump.luna.StateContext;
import org.classdump.luna.Table;
import org.classdump.luna.Variable;
import org.classdump.luna.compiler.CompilerChunkLoader;
import org.classdump.luna.env.RuntimeEnvironment;
import org.classdump.luna.env.RuntimeEnvironments;
import org.classdump.luna.exec.CallException;
import org.classdump.luna.exec.CallPausedException;
import org.classdump.luna.exec.DirectCallExecutor;
import org.classdump.luna.impl.DefaultTable;
import org.classdump.luna.impl.ImmutableTable;
import org.classdump.luna.impl.StateContexts;
import org.classdump.luna.lib.*;
import org.classdump.luna.load.ChunkLoader;
import org.classdump.luna.load.LoaderException;
import org.classdump.luna.runtime.ExecutionContext;
import org.classdump.luna.runtime.LuaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starloco.locos.game.world.World;
import org.starloco.locos.game.world.World.Couple;
import org.starloco.locos.util.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class ScriptVM {
    public static final Logger logger = LoggerFactory.getLogger("Script");

    protected final Table env;
    protected final StateContext state;
    protected final ChunkLoader loader;
    protected final DirectCallExecutor executor = DirectCallExecutor.newExecutor();

    protected final Object _vmLock = new Object(); // Find a better way if performances become an issue

    protected ScriptVM(String name) throws CallException, LoaderException, IOException, CallPausedException, InterruptedException {
        this.state = StateContexts.newDefaultInstance();
        RuntimeEnvironment rtEnv = RuntimeEnvironments.system();

        this.loader = CompilerChunkLoader.of(name);
        this.env = state.newTable();
        BasicLib.installInto(state, env, rtEnv, null);
        ModuleLib.installInto(state, env, rtEnv, this.loader, null);
        CoroutineLib.installInto(state, env);
        StringLib.installInto(state, env);
        MathLib.installInto(state, env);
        TableLib.installInto(state, env);

        this.env.rawset("JLogF", new LogF());
        this.env.rawset("loadDir", new LoadDir());
        this.env.rawset("World", World.world.scripted());
        this.loadData();
    }

    protected void loadData() throws CallException, LoaderException, IOException, CallPausedException, InterruptedException{
        runFile(Paths.get("scripts", "Common.lua"));
    }

    protected void runDirectory(Path path) {
        try (Stream<Path> paths = Files.walk(path)) {
            Iterator<Path> it = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".lua")).iterator();
            while(it.hasNext()) {
                Path p = it.next();
                try {
                    this.runFile(p);
                }catch(Exception e) {
                    ScriptVM.logger.error("cannot load file: "+p, e);
                }
            }
        } catch(Exception e){
            ScriptVM.logger.error("cannot load directory", e);
        }
    }

    protected void runFile(Path path) throws IOException, LoaderException, CallException, CallPausedException, InterruptedException {
        byte[] bytes = Files.readAllBytes(path);

        LuaFunction<?,?,?,?,?> fn = loader.loadTextChunk(new Variable(env), path.toString(), new String(bytes)); // May need to UTF-8 decode

        this.executor.call(this.state, fn);
    }
    public Object[] call(Object val, Object... args) {
        synchronized (_vmLock) {
            try {
                return this.executor.call(this.state, val, args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    public Object[] run(String code) {
        synchronized (_vmLock) {
            try {
                LuaFunction<?,?,?,?,?> fn = loader.loadTextChunk(new Variable(env), "command", code); // May need to UTF-8 decode

                return this.executor.call(this.state, fn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Object recursiveGet(Table t, Object key) {
        if(t == null) return null;

        Object v = t.rawget(key);
        if(v != null) return v;
        Object mtIndex = t.getMetatable().rawget("__index");
        if(!(mtIndex instanceof Table)) return null;

        return recursiveGet((Table) mtIndex, key);
    }


    static class LogF extends AbstractLibFunction {
        @Override
        protected String name() { return "JLogF"; }

        @Override
        public void invoke(ExecutionContext context, ArgumentIterator args) {
            ByteString fmt = args.nextString();
            logger.info(fmt.toString(), args.copyRemaining());
        }
    }

    class LoadDir extends AbstractLibFunction {
        @Override
        protected String name() { return "RequireDir"; }

        @Override
        public void invoke(ExecutionContext context, ArgumentIterator args) {
            Path path = Paths.get("scripts", args.nextStrictString().toString());

            runDirectory(path);

            context.getReturnBuffer().setTo(true);
        }
    }


    public static int rawOptionalInt(Table v, Object key, int val) {
        Object n = v.rawget(key);
        if(n==null) return val;
        return ((Long)v.rawget(key)).intValue();
    }

    public static int rawInt(Table v, Object key) {
        return ((Long)v.rawget(key)).intValue();
    }
    public static int rawInt(Table v, long key) {
        return ((Long)v.rawget(key)).intValue();
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> listFromLuaTable(Table t) {
        ArrayList<T> out = new ArrayList<>();

        long len = t.rawlen();
        for(int i=1;i<=len;i++){
            out.add((T)t.rawget(i));
        }

        return out;
    }

    @SuppressWarnings("unchecked")
    public static <K,V> Pair<K,V> toPair(Table t) {
        return new Pair<>((K)t.rawget(1L), (V)t.rawget(2L));
    }

    public static List<Pair<Integer,Integer>> listOfIntPairs(Table t) {
        if(t == null) return null;

        long len = t.rawlen();
        List<Pair<Integer,Integer>> out = new LinkedList<>();

        for(long i=0;i<len;i++){
            out.add(ScriptVM.<Long,Long>toPair((Table)t.rawget(i+1)).map(Long::intValue,Long::intValue));
        }

        return out;
    }

    public static List<String> listOfString(Table t) {
        if(t == null) return null;

        long len = t.rawlen();
        List<String> out = new LinkedList<>();

        for(long i=0; i<len; i++) {
            ByteString bs = (ByteString)t.rawget(i+1);
            out.add(bs.toString());
        }

        return out;
    }

    public static List<Integer> intsFromLuaTable(Table t) {
        ArrayList<Integer> out = new ArrayList<>();
        if(t == null) return out;

        long len = t.rawlen();
        for(int i=1;i<=len;i++){
            int a = rawInt (t, i);
            out.add(a);
        }

        return out;
    }

    public static int[] intArrayFromLuaTable(Table t) {
        if(t == null) return null;

        long len = t.rawlen();
        int[] out = new int[(int)len];

        for(int i=0;i<len;i++){
            out[i] = rawInt (t, i+1);
        }

        return out;
    }

    public static long[] longArrayFromLuaTable(Table t) {
        if(t == null) return null;

        long len = t.rawlen();
        long[] out = new long[(int)len];

        for(int i=0;i<len;i++){
            out[i] = (Long)t.rawget(i+1);
        }

        return out;
    }

    public static Table ItemStack(Couple<Integer,Integer> stack) {
        return new ImmutableTable.Builder()
            .add("itemID", stack.first)
            .add("quantity", stack.second)
            .build();
    }
    public static Couple<Integer,Integer> ItemStackFromLua(Table t) {
        int id = rawInt(t, "itemID");
        int qua = rawInt(t, "quantity");
        return new Couple<>(id, qua);
    }

    public static <K,V> Map<K,V> mapFromScript(Table t, Function<Object,K> keyMapper, Function<Object,V> valMapper) {
        Map<K,V> out = new HashMap<>();

        for(Object key = t.initialKey(); key != null; key = t.successorKeyOf(key)) {
            Object val = t.rawget(key);

            out.put(keyMapper.apply(key), valMapper.apply(val));
        }

        return out;
    }

    public static Table scriptedValsTable(Collection<? extends Scripted<?>> vals) {
        return scriptedValsTable(vals.stream());
    }

    public static Table scriptedValsTable(Stream<? extends Scripted<?>> vals) {
        DefaultTable out = new DefaultTable();

        int i=1;
        Iterator<? extends Scripted<?>> it = vals.iterator();
        while(it.hasNext()) {
            out.rawset(i, it.next().scripted());
            i++;
        }
        return out;
    }

    public static <T> Table listOf(Stream<T> vals) {
        DefaultTable out = new DefaultTable();
        int i=1;
        Iterator<T> it = vals.iterator();
        while(it.hasNext()) {
            out.rawset(i, it.next());
            i++;
        }
        return out;
    }
}
