-- Catalogo inicial. Categorias editables por el usuario, empezando simple (CLAUDE §5).

insert into merchant (name, tax_id)
values ('Mercadona', 'A-46103834');

insert into category (name)
values ('Alimentación'),
       ('Bebidas'),
       ('Limpieza'),
       ('Higiene y cuidado personal'),
       ('Hogar'),
       ('Otros');
