package mvega.dev.cuentas.mapper;

public interface IMapper <I, O>{
    public O map(I in);
    //public O mapExist(I in, O inExist);
}