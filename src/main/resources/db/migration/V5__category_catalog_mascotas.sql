-- Ajuste del catálogo de categorías al acordado: se añade Mascotas y se acorta el nombre
-- de higiene. Se hace con `where` sobre el nombre para que sea idempotente y no falle si la
-- base ya estaba al día.

update category
set name = 'Higiene personal'
where name = 'Higiene y cuidado personal';

insert into category (name)
select 'Mascotas'
where not exists (select 1 from category where name = 'Mascotas');
