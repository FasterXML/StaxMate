
package org.codehaus.staxmate.dom;

import java.util.Iterator;
import java.util.List;

/**
 *
 * @author elbosso
 */
public class Tupel<T,U> extends java.lang.Object
{
    private T lefty;
    private U righty;

    public Tupel(T lefty, U righty)
    {
        super();
        if((lefty==null)&&(righty==null))
            throw new java.lang.IllegalArgumentException("both arguments must not be null!");
        this.lefty = lefty;
        this.righty = righty;
    }

    public T getLefty()
    {
        return lefty;
    }

    public U getRighty()
    {
        return righty;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 67 * hash + (this.lefty != null ? this.lefty.hashCode() : 0);
        hash = 67 * hash + (this.righty != null ? this.righty.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final Tupel<T, U> other = (Tupel<T, U>) obj;
        if (this.lefty != other.lefty && (this.lefty == null || !this.lefty.equals(other.lefty)))
        {
            return false;
        }
        if (this.righty != other.righty && (this.righty == null || !this.righty.equals(other.righty)))
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        java.lang.StringBuffer buf=new java.lang.StringBuffer();
        buf.append(lefty!=null?lefty.toString():"null");
        buf.append(":/:");
        buf.append(righty!=null?righty.toString():"null");
        return buf.toString();
    }

    public static <T,U>java.util.Collection<T> getLeftAsList(java.util.Collection<Tupel<T,U> > tupels)
    {
        java.util.LinkedList<T> rv=new java.util.LinkedList();
        for(Tupel<T,U> tupel:tupels)
        {
            rv.add(tupel.getLefty());
        }
        return rv;
    }
    public static <T> Tupel<T,T> swappedNewOne(Tupel<T,T> input)
    {
        return new Tupel(input.getRighty(),input.getLefty());
    }
    public static <T> void swappedInPlace(Tupel<T,T> input)
    {
        T latch=input.lefty;
        input.lefty=input.righty;
        input.righty=latch;
    }
    public static <T,U>java.util.Collection<U> getRightAsList(java.util.Collection<Tupel<T,U> > tupels)
    {
        java.util.LinkedList<U> rv=new java.util.LinkedList();
        for(Tupel<T,U> tupel:tupels)
        {
            rv.add(tupel.getRighty());
        }
        return rv;
    }
    public static <T>Iterable<Tupel<T,T> > getAllPairs(java.util.List<T> input)
    {
        return new PairIterable(input);
    }
    private static class PairIterable<T> extends java.lang.Object implements Iterable<Tupel<T,T>>
    {
        private final PairIterator<T> pairIterator;

        public PairIterable(List<T> input)
        {
            super();
            this.pairIterator=new PairIterator(input);
        }

        @Override
        public Iterator<Tupel<T,T>> iterator()
        {
            return pairIterator;
        }
    }

    private static class PairIterator<T> extends java.lang.Object implements Iterator<Tupel<T,T>>
    {
        private final java.util.List<T> input;
        private int indexa=0;
        private int indexb=indexa+1;

        public PairIterator(List<T> input)
        {
            this.input = input;
        }

        @Override
        public boolean hasNext()
        {
            return ((indexa<input.size()&&(indexb< input.size())));
        }

        @Override
        public Tupel<T,T> next()
        {
            Tupel<T,T> t=new Tupel(input.get(indexa),input.get(indexb));
            ++indexb;
            if(indexb>= input.size())
            {
                ++indexa;
                indexb=indexa+1;
            }
            return t;
        }
    }

}
