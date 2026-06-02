ALTER TABLE category
    DROP CONSTRAINT category_tenant_id_fkey,
    ADD  CONSTRAINT category_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES tenant (id) ON DELETE CASCADE;

ALTER TABLE expense
    DROP CONSTRAINT expense_tenant_id_fkey,
    ADD  CONSTRAINT expense_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES tenant (id) ON DELETE CASCADE;

ALTER TABLE income
    DROP CONSTRAINT income_tenant_id_fkey,
    ADD  CONSTRAINT income_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES tenant (id) ON DELETE CASCADE;
