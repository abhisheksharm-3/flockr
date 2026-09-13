-- Baseline: every function in the public schema, as the database actually defines them.
--
-- The project had fifty-two RPCs live and two in version control. Everything else existed
-- only in the dashboard, which meant the security-critical authorization logic in each one
-- had never been reviewed and the backend could not be rebuilt from this repository.
-- Generated with pg_get_functiondef, so this is the real source, not a reconstruction.

-- accept_house_invitation
CREATE OR REPLACE FUNCTION public.accept_house_invitation(p_invitation_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
  v_house_id uuid;
  v_invitee_email text;
  v_current_email text;
BEGIN
  SELECT house_id, invitee_email INTO v_house_id, v_invitee_email
  FROM house_invitations
  WHERE id = p_invitation_id AND status = 'pending';
  IF v_house_id IS NULL THEN
    RAISE EXCEPTION 'Invitation not found or already processed';
  END IF;
  SELECT email INTO v_current_email FROM profiles WHERE id = auth.uid();
  IF v_current_email IS NULL OR v_current_email <> v_invitee_email THEN
    RAISE EXCEPTION 'You are not authorized to accept this invitation';
  END IF;
  INSERT INTO house_members (house_id, user_id, role)
  VALUES (v_house_id, auth.uid(), 'Member');
  UPDATE house_invitations SET status = 'accepted' WHERE id = p_invitation_id;
END;
$function$;

-- add_owner_as_member
CREATE OR REPLACE FUNCTION public.add_owner_as_member()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    -- Use ON CONFLICT to handle duplicates gracefully
    INSERT INTO house_members (house_id, user_id, joined_at)
    VALUES (NEW.id, NEW.owner_id, now())
    ON CONFLICT (house_id, user_id) DO NOTHING;
    
    RETURN NEW;
END;
$function$;

-- auth_is_house_admin
CREATE OR REPLACE FUNCTION public.auth_is_house_admin(check_house_id uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  SELECT EXISTS (
    SELECT 1 FROM public.house_members 
    WHERE house_id = check_house_id 
    AND user_id = auth.uid() 
    AND role IN ('Owner', 'Admin')
    AND is_active = true
  );
$function$;

-- auth_is_house_member
CREATE OR REPLACE FUNCTION public.auth_is_house_member(check_house_id uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  SELECT EXISTS (
    SELECT 1 FROM public.house_members 
    WHERE house_id = check_house_id 
    AND user_id = auth.uid() 
    AND is_active = true
  );
$function$;

-- auth_user_house_ids
CREATE OR REPLACE FUNCTION public.auth_user_house_ids()
 RETURNS SETOF uuid
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  SELECT house_id FROM house_members 
  WHERE user_id = auth.uid() 
  AND is_active = true;
$function$;

-- auto_cleanup_house_data
CREATE OR REPLACE FUNCTION public.auto_cleanup_house_data()
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    -- Clean up old notifications (already done in main fixes)
    DELETE FROM notifications
    WHERE is_read = true AND created_at < NOW() - INTERVAL '30 days';

    DELETE FROM notifications
    WHERE is_read = false AND created_at < NOW() - INTERVAL '90 days';

    -- Clean up old audit logs (keep 1 year)
    DELETE FROM house_audit_log
    WHERE created_at < NOW() - INTERVAL '1 year';

    -- Clean up expired invitations
    DELETE FROM house_invitations
    WHERE status = 'pending' AND created_at < NOW() - INTERVAL '30 days';

    -- Clean up inactive members (who left more than 1 year ago)
    DELETE FROM house_members
    WHERE is_active = false AND left_at < NOW() - INTERVAL '1 year';

    -- Reset rate limit records older than 24 hours
    DELETE FROM invitation_rate_limit
    WHERE window_start < NOW() - INTERVAL '24 hours';

    RAISE NOTICE 'Auto cleanup completed successfully';
END;
$function$;

-- calculate_next_due_date
CREATE OR REPLACE FUNCTION public.calculate_next_due_date(p_last_paid_date date, p_due_day integer, p_frequency text, p_custom_frequency_days integer DEFAULT NULL::integer)
 RETURNS date
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
DECLARE
    v_next_date DATE;
    v_base_date DATE;
    v_target_month DATE;
    v_days_in_month INTEGER;
BEGIN
    v_base_date := COALESCE(p_last_paid_date, CURRENT_DATE);

    CASE p_frequency
        WHEN 'daily' THEN
            v_next_date := v_base_date + INTERVAL '1 day';
        WHEN 'weekly' THEN
            v_next_date := v_base_date + INTERVAL '1 week';
        WHEN 'biweekly' THEN
            v_next_date := v_base_date + INTERVAL '2 weeks';
        WHEN 'monthly' THEN
            v_target_month := (DATE_TRUNC('month', v_base_date) + INTERVAL '1 month')::date;
            v_days_in_month := EXTRACT(DAY FROM (v_target_month + INTERVAL '1 month' - INTERVAL '1 day'))::int;
            v_next_date := v_target_month + (LEAST(GREATEST(p_due_day, 1), v_days_in_month) - 1);
        WHEN 'quarterly' THEN
            v_next_date := v_base_date + INTERVAL '3 months';
        WHEN 'semiannual' THEN
            v_next_date := v_base_date + INTERVAL '6 months';
        WHEN 'annual' THEN
            v_next_date := v_base_date + INTERVAL '1 year';
        WHEN 'custom' THEN
            IF p_custom_frequency_days IS NOT NULL THEN
                v_next_date := v_base_date + (p_custom_frequency_days || ' days')::INTERVAL;
            ELSE
                v_next_date := v_base_date + INTERVAL '1 month';
            END IF;
        ELSE
            v_target_month := (DATE_TRUNC('month', v_base_date) + INTERVAL '1 month')::date;
            v_days_in_month := EXTRACT(DAY FROM (v_target_month + INTERVAL '1 month' - INTERVAL '1 day'))::int;
            v_next_date := v_target_month + (LEAST(GREATEST(p_due_day, 1), v_days_in_month) - 1);
    END CASE;

    RETURN v_next_date;
END;
$function$;

-- calculate_next_payment_date
CREATE OR REPLACE FUNCTION public.calculate_next_payment_date(p_frequency character varying, p_last_payment_date date, p_first_payment_date date DEFAULT NULL::date)
 RETURNS date
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
DECLARE
    v_next_date DATE;
    v_base_date DATE;
BEGIN
    -- If no payments made yet and first_payment_date is set, return it
    IF p_last_payment_date IS NULL AND p_first_payment_date IS NOT NULL THEN
        RETURN p_first_payment_date;
    END IF;

    -- Use last payment date or current date as base
    v_base_date := COALESCE(p_last_payment_date, CURRENT_DATE);

    -- Calculate based on frequency
    CASE p_frequency
        WHEN 'Daily' THEN
            v_next_date := v_base_date + INTERVAL '1 day';
        WHEN 'Weekly' THEN
            v_next_date := v_base_date + INTERVAL '1 week';
        WHEN 'Monthly' THEN
            v_next_date := v_base_date + INTERVAL '1 month';
        WHEN 'Yearly' THEN
            v_next_date := v_base_date + INTERVAL '1 year';
        ELSE
            v_next_date := v_base_date + INTERVAL '1 month';
    END CASE;

    RETURN v_next_date;
END;
$function$;

-- cancel_invitation
CREATE OR REPLACE FUNCTION public.cancel_invitation(p_house_id uuid, p_email text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
begin
  -- Check permissions (Normalized roles)
  if not exists (
    select 1 from public.house_members
    where house_id = p_house_id 
    and user_id = auth.uid()
    and role in ('Owner', 'Admin')
    and is_active = true
  ) then
    raise exception 'Insufficient permissions to cancel invitations';
  end if;

  delete from public.house_invitations
  where house_id = p_house_id and invitee_email = p_email;
end;
$function$;

-- check_invitation_rate_limit
CREATE OR REPLACE FUNCTION public.check_invitation_rate_limit(p_house_id uuid)
 RETURNS boolean
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_user_id UUID;
    v_invitations_sent INTEGER;
    v_window_start TIMESTAMP WITH TIME ZONE;
    v_limit INTEGER := 10; -- Max 10 invitations per hour
BEGIN
    v_user_id := auth.uid();

    -- Get or create rate limit record
    INSERT INTO invitation_rate_limit (user_id, house_id, invitations_sent, window_start)
    VALUES (v_user_id, p_house_id, 0, NOW())
    ON CONFLICT (user_id, house_id)
    DO UPDATE SET invitations_sent = invitation_rate_limit.invitations_sent
    RETURNING invitations_sent, window_start INTO v_invitations_sent, v_window_start;

    -- Reset counter if window has passed (1 hour)
    IF v_window_start < NOW() - INTERVAL '1 hour' THEN
        UPDATE invitation_rate_limit
        SET invitations_sent = 0,
            window_start = NOW()
        WHERE user_id = v_user_id AND house_id = p_house_id;
        RETURN true;
    END IF;

    -- Check if limit exceeded
    IF v_invitations_sent >= v_limit THEN
        RETURN false;
    END IF;

    -- Increment counter
    UPDATE invitation_rate_limit
    SET invitations_sent = invitations_sent + 1
    WHERE user_id = v_user_id AND house_id = p_house_id;

    RETURN true;
END;
$function$;

-- check_member_limit
CREATE OR REPLACE FUNCTION public.check_member_limit()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
DECLARE
    v_max_members INTEGER;
    v_current_count INTEGER;
BEGIN
    -- Get max members for this house
    SELECT max_members INTO v_max_members
    FROM houses
    WHERE id = NEW.house_id;

    -- Count current members
    SELECT COUNT(*) INTO v_current_count
    FROM house_members
    WHERE house_id = NEW.house_id;

    -- Check if limit is reached
    IF v_current_count >= v_max_members THEN
        RAISE EXCEPTION 'House has reached maximum member limit of %', v_max_members;
    END IF;

    RETURN NEW;
END;
$function$;

-- cleanup_old_notifications
CREATE OR REPLACE FUNCTION public.cleanup_old_notifications()
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    -- Delete read notifications older than 30 days
    DELETE FROM notifications
    WHERE is_read = true
      AND created_at < NOW() - INTERVAL '30 days';

    -- Delete unread notifications older than 90 days
    DELETE FROM notifications
    WHERE is_read = false
      AND created_at < NOW() - INTERVAL '90 days';
END;
$function$;

-- create_default_house_config
CREATE OR REPLACE FUNCTION public.create_default_house_config()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
BEGIN
    INSERT INTO house_config (house_id, currency_code)
    VALUES (NEW.id, 'USD')
    ON CONFLICT (house_id) DO NOTHING;
    RETURN NEW;
END;
$function$;

-- create_house_with_owner
CREATE OR REPLACE FUNCTION public.create_house_with_owner(p_name text, p_owner_id uuid, p_invite_code text, p_address text DEFAULT NULL::text, p_latitude double precision DEFAULT NULL::double precision, p_longitude double precision DEFAULT NULL::double precision, OUT out_house_id uuid, OUT out_house_name text, OUT out_invite_code text)
 RETURNS record
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_new_house_id uuid;
BEGIN
    -- Verify the caller is the owner
    IF p_owner_id != auth.uid() THEN
        RAISE EXCEPTION 'Unauthorized: You can only create houses for yourself';
    END IF;

    -- Insert house
    INSERT INTO houses (name, owner_id, invite_code, address, latitude, longitude)
    VALUES (p_name, p_owner_id, p_invite_code, p_address, p_latitude, p_longitude)
    RETURNING id INTO v_new_house_id;

    -- Add owner as member with 'Owner' role
    INSERT INTO house_members (house_id, user_id, role, joined_at)
    VALUES (v_new_house_id, p_owner_id, 'Owner', NOW())
    ON CONFLICT (house_id, user_id) DO UPDATE
    SET role = 'Owner';

    -- Create default house config
    -- FIX: Removed currency_symbol (schema expects app to handle logic based on code)
    INSERT INTO house_config (house_id, currency_code)
    VALUES (v_new_house_id, 'USD')
    ON CONFLICT (house_id) DO NOTHING;

    -- Set output parameters
    out_house_id := v_new_house_id;
    out_house_name := p_name;
    out_invite_code := p_invite_code;
END;
$function$;

-- create_notification
CREATE OR REPLACE FUNCTION public.create_notification(p_user_id uuid, p_house_id uuid, p_title text, p_message text, p_type text, p_data text DEFAULT '{}'::text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
begin
    if coalesce(auth.role(), '') in ('authenticated', 'anon')
       and not public.auth_is_house_member(p_house_id) then
        raise exception 'Not a member of this house' using errcode = '42501';
    end if;

    insert into notifications (
        user_id,
        house_id,
        title,
        message,
        type,
        data,
        is_read,
        created_at
    ) values (
        p_user_id,
        p_house_id,
        p_title,
        p_message,
        p_type,
        p_data::jsonb,
        false,
        now()
    );
end;
$function$;

-- create_notification_for_house
CREATE OR REPLACE FUNCTION public.create_notification_for_house(p_house_id uuid, p_title text, p_message text, p_type text, p_data text DEFAULT '{}'::text, p_exclude_user_id uuid DEFAULT NULL::uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
begin
    if coalesce(auth.role(), '') in ('authenticated', 'anon')
       and not public.auth_is_house_member(p_house_id) then
        raise exception 'Not a member of this house' using errcode = '42501';
    end if;

    insert into notifications (
        user_id,
        house_id,
        title,
        message,
        type,
        data,
        is_read,
        created_at
    )
    select
        hm.user_id,
        p_house_id,
        p_title,
        p_message,
        p_type,
        p_data,
        false,
        now()
    from house_members hm
    where hm.house_id = p_house_id
      and hm.is_active = true
      and (p_exclude_user_id is null or hm.user_id != p_exclude_user_id);
end;
$function$;

-- create_one_time_expense
CREATE OR REPLACE FUNCTION public.create_one_time_expense(p_house_id uuid, p_paid_by uuid, p_name text, p_amount numeric, p_category text, p_date date, p_notes text, p_splits jsonb)
 RETURNS uuid
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_expense_id uuid;
    v_split jsonb;
    v_split_total numeric;
BEGIN
    IF NOT auth_is_house_member(p_house_id) THEN
        RAISE EXCEPTION 'Not a member of this house';
    END IF;
    IF NOT is_house_member(p_house_id, p_paid_by) THEN
        RAISE EXCEPTION 'Payer must be a member of this house';
    END IF;
    IF p_amount IS NULL OR p_amount <= 0 THEN
        RAISE EXCEPTION 'Amount must be greater than zero';
    END IF;

    SELECT COALESCE(SUM((s->>'amount')::numeric), 0)
    INTO v_split_total
    FROM jsonb_array_elements(COALESCE(p_splits, '[]'::jsonb)) s;

    IF v_split_total < 0 OR v_split_total > p_amount + 0.005 THEN
        RAISE EXCEPTION 'Split amounts (%) cannot exceed the expense amount (%)', v_split_total, p_amount;
    END IF;

    INSERT INTO one_time_expenses (
        house_id, paid_by, name, amount, category, date, notes
    ) VALUES (
        p_house_id, p_paid_by, p_name, p_amount, p_category, p_date, p_notes
    ) RETURNING id INTO v_expense_id;

    FOR v_split IN SELECT * FROM jsonb_array_elements(COALESCE(p_splits, '[]'::jsonb))
    LOOP
        INSERT INTO expense_splits (expense_id, user_id, amount_owed)
        VALUES (v_expense_id, (v_split->>'user_id')::uuid, (v_split->>'amount')::numeric);
    END LOOP;

    RETURN v_expense_id;
END;
$function$;

-- delete_all_notifications
CREATE OR REPLACE FUNCTION public.delete_all_notifications(p_user_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
begin
    if p_user_id is distinct from auth.uid() then
        raise exception 'Can only delete your own notifications' using errcode = '42501';
    end if;

    delete from notifications
    where user_id = p_user_id;
end;
$function$;

-- delete_house
CREATE OR REPLACE FUNCTION public.delete_house(p_house_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
begin
  -- Check if the user is the owner of the house
  if not exists (
    select 1 from houses
    where id = p_house_id and owner_id = auth.uid()
  ) then
    raise exception 'Only the house owner can delete the house';
  end if;

  -- Delete related data explicitly
  delete from house_invitations where house_id = p_house_id;
  delete from house_members where house_id = p_house_id;
  delete from house_audit_log where house_id = p_house_id;
  delete from house_config where house_id = p_house_id;
  delete from chores where house_id = p_house_id;
  delete from one_time_expenses where house_id = p_house_id;
  delete from recurring_expenses where house_id = p_house_id;
  delete from per_diem_config where house_id = p_house_id;
  
  -- Handle Per Diem Entries (linked to config)
  delete from per_diem_entries where config_id in (select id from per_diem_config where house_id = p_house_id);
  
  -- CORRECTED TABLE NAME (was shopping_list_items)
  delete from shopping_items where house_id = p_house_id;
  
  delete from transactions where house_id = p_house_id;
  delete from notifications where house_id = p_house_id;
  
  -- Finally delete the house
  delete from houses where id = p_house_id;
end;
$function$;

-- delete_notification
CREATE OR REPLACE FUNCTION public.delete_notification(p_notification_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
begin
  delete from notifications
  where id = p_notification_id and user_id = auth.uid();
end;
$function$;

-- enforce_role_change_admin_only
CREATE OR REPLACE FUNCTION public.enforce_role_change_admin_only()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    IF NEW.role IS DISTINCT FROM OLD.role AND NOT auth_is_house_admin(NEW.house_id) THEN
        RAISE EXCEPTION 'Only house admins can change member roles';
    END IF;
    RETURN NEW;
END;
$function$;

-- generate_invite_code
CREATE OR REPLACE FUNCTION public.generate_invite_code()
 RETURNS text
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
DECLARE
    chars TEXT := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; -- Removed confusing chars
    result TEXT := '';
    i INTEGER;
BEGIN
    FOR i IN 1..6 LOOP
        result := result || substr(chars, floor(random() * length(chars) + 1)::int, 1);
    END LOOP;
    RETURN result;
END;
$function$;

-- get_debt_breakdown
CREATE OR REPLACE FUNCTION public.get_debt_breakdown(p_house_id uuid, p_payer_id uuid, p_payee_id uuid)
 RETURNS TABLE(expense_id uuid, expense_name text, date date, amount_owed numeric, total_amount numeric)
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
BEGIN
    RETURN QUERY
    SELECT 
        ote.id,
        ote.name,
        ote.date,
        es.amount_owed,
        ote.amount
    FROM expense_splits es
    JOIN one_time_expenses ote ON es.expense_id = ote.id
    WHERE ote.house_id = p_house_id
    AND ote.paid_by = p_payee_id 
    AND es.user_id = p_payer_id 
    AND es.is_settled = false 
    AND ote.category != 'Settlement' -- FIX: Don't show settlements in debt list
    ORDER BY ote.date DESC;
END;
$function$;

-- get_house_by_invite_code
CREATE OR REPLACE FUNCTION public.get_house_by_invite_code(code text)
 RETURNS TABLE(id uuid, name text, header_image_url text)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    -- Only return minimal info - don't expose owner_id, invite_code, or other sensitive data
    RETURN QUERY
    SELECT
        h.id,
        h.name,
        h.header_image_url
    FROM houses h
    WHERE h.invite_code = UPPER(TRIM(code))
    LIMIT 1;
END;
$function$;

-- get_house_by_invite_code_v2
CREATE OR REPLACE FUNCTION public.get_house_by_invite_code_v2(code text)
 RETURNS TABLE(id uuid, name text, header_image_url text, owner_name text, member_count bigint)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    RETURN QUERY
    SELECT
        h.id,
        h.name,
        h.header_image_url,
        p.full_name as owner_name,
        (SELECT COUNT(*) FROM house_members hm WHERE hm.house_id = h.id AND hm.is_active = true) as member_count
    FROM houses h
    JOIN profiles p ON h.owner_id = p.id
    WHERE h.invite_code = UPPER(TRIM(code))
    LIMIT 1;
END;
$function$;

-- get_house_members_with_profiles
CREATE OR REPLACE FUNCTION public.get_house_members_with_profiles(p_house_id uuid)
 RETURNS TABLE(user_id uuid, role text, joined_at timestamp with time zone, email text, full_name text, avatar_url text)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
begin
  if not public.auth_is_house_member(p_house_id) then
    raise exception 'Not a member of this house' using errcode = '42501';
  end if;

  return query
  select
    hm.user_id,
    hm.role,
    hm.joined_at,
    p.email,
    p.full_name,
    p.avatar_url
  from house_members hm
  join profiles p on hm.user_id = p.id
  where hm.house_id = p_house_id;
end;
$function$;

-- get_houses_enriched
CREATE OR REPLACE FUNCTION public.get_houses_enriched(p_user_id uuid, p_month date)
 RETURNS TABLE(id uuid, name text, owner_id uuid, invite_code text, address text, latitude double precision, longitude double precision, header_image_url text, member_count integer, monthly_expense numeric, currency_code text)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    RETURN QUERY
    SELECT 
        h.id,
        h.name,
        h.owner_id,
        h.invite_code,
        h.address,
        h.latitude,
        h.longitude,
        h.header_image_url,
        COALESCE(mc.member_count, 0)::INTEGER AS member_count,
        COALESCE(me.monthly_expense, 0)::NUMERIC AS monthly_expense,
        COALESCE(hc.currency_code, 'USD')::TEXT AS currency_code
    FROM houses h
    INNER JOIN house_members hm ON hm.house_id = h.id AND hm.user_id = p_user_id
    LEFT JOIN (
        -- Member count subquery
        SELECT 
            house_id, 
            COUNT(*)::INTEGER AS member_count
        FROM house_members
        GROUP BY house_id
    ) mc ON mc.house_id = h.id
    LEFT JOIN (
        -- Monthly expense subquery
        SELECT 
            house_id,
            COALESCE(SUM(amount), 0) AS monthly_expense
        FROM one_time_expenses
        WHERE date >= p_month 
          AND date < (p_month + INTERVAL '1 month')::DATE
        GROUP BY house_id
    ) me ON me.house_id = h.id
    LEFT JOIN house_config hc ON hc.house_id = h.id
    ORDER BY h.name;
END;
$function$;

-- get_monthly_summary
CREATE OR REPLACE FUNCTION public.get_monthly_summary(p_house_id uuid, p_month date)
 RETURNS TABLE(total_expenses numeric, recurring_expenses numeric, one_time_expenses numeric, per_diem_expenses numeric)
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
DECLARE
    v_one_time_raw NUMERIC; -- Total from one_time_expenses (Includes Payments, Excludes Settlements)
    v_recurring_paid NUMERIC; -- Total from payment_history (Actual Paid Bills)
    v_per_diem_total NUMERIC;
    v_start_date DATE;
    v_end_date DATE;
BEGIN
    -- Calculate start and end of month
    v_start_date := date_trunc('month', p_month)::date;
    v_end_date := (v_start_date + interval '1 month')::date;

    -- 1. One-Time Expenses (Raw Sum)
    -- This table contains BOTH true one-time items AND recurring bill payments.
    -- FIX: Exclude 'Settlement' category
    SELECT COALESCE(SUM(amount), 0)
    INTO v_one_time_raw
    FROM one_time_expenses
    WHERE house_id = p_house_id
    AND date >= v_start_date 
    AND date < v_end_date
    AND category != 'Settlement'; -- EXCLUDE SETTLEMENTS

    -- 2. Recurring Payments (Cash Basis)
    -- We ONLY count what is in payment_history for this month.
    SELECT COALESCE(SUM(amount), 0)
    INTO v_recurring_paid
    FROM payment_history
    WHERE recurring_expense_id IN (
        SELECT id FROM recurring_expenses WHERE house_id = p_house_id
    )
    AND payment_date >= v_start_date
    AND payment_date < v_end_date;

    -- 3. Per Diem
    SELECT COALESCE(SUM(pde.quantity * pdc.rate), 0)
    INTO v_per_diem_total
    FROM per_diem_entries pde
    JOIN per_diem_config pdc ON pde.config_id = pdc.id
    WHERE pdc.house_id = p_house_id
    AND pde.date >= v_start_date 
    AND pde.date < v_end_date;

    -- Return Values
    -- Total: Raw One-Time (includes payments) + Per Diem.
    -- Recurring: Just the Paid Amount.
    -- One-Time: Raw One-Time - Paid Amount (isolates true one-offs).
    
    RETURN QUERY SELECT
        (v_one_time_raw + v_per_diem_total)::NUMERIC as total_expenses,
        v_recurring_paid::NUMERIC as recurring_expenses,
        (v_one_time_raw - v_recurring_paid)::NUMERIC as one_time_expenses,
        v_per_diem_total::NUMERIC as per_diem_expenses;
END;
$function$;

-- get_my_pending_invitations_with_details
CREATE OR REPLACE FUNCTION public.get_my_pending_invitations_with_details()
 RETURNS TABLE(id uuid, house_id uuid, house_name text, inviter_id uuid, invitee_email text, status text, created_at timestamp with time zone)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
  current_user_email text;
BEGIN
  -- Get email of executing user
  SELECT email INTO current_user_email FROM profiles WHERE id = auth.uid();
  RETURN QUERY
  SELECT
    hi.id,
    hi.house_id,
    h.name as house_name,
    hi.inviter_id,
    hi.invitee_email,
    hi.status,
    hi.created_at
  FROM house_invitations hi
  JOIN houses h ON h.id = hi.house_id
  WHERE hi.invitee_email = current_user_email
  AND hi.status = 'pending';
END;
$function$;

-- get_pairwise_balances
CREATE OR REPLACE FUNCTION public.get_pairwise_balances(p_house_id uuid, p_user_id uuid)
 RETURNS TABLE(user_id uuid, full_name text, balance numeric)
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    IF NOT auth_is_house_member(p_house_id) THEN
        RAISE EXCEPTION 'Access denied';
    END IF;

    RETURN QUERY
    SELECT
        p.id AS user_id,
        p.full_name,
        (
            -- What this member owes the caller (caller paid, member is a splitee)
            COALESCE((
                SELECT SUM(es.amount_owed)
                FROM expense_splits es
                JOIN one_time_expenses ote ON ote.id = es.expense_id
                WHERE ote.house_id = p_house_id
                  AND ote.paid_by = p_user_id
                  AND es.user_id = p.id
                  AND es.is_settled = false
            ), 0)
            -
            -- What the caller owes this member (member paid, caller is a splitee)
            COALESCE((
                SELECT SUM(es.amount_owed)
                FROM expense_splits es
                JOIN one_time_expenses ote ON ote.id = es.expense_id
                WHERE ote.house_id = p_house_id
                  AND ote.paid_by = p.id
                  AND es.user_id = p_user_id
                  AND es.is_settled = false
            ), 0)
        )::NUMERIC AS balance
    FROM profiles p
    JOIN house_members hm ON hm.user_id = p.id
    WHERE hm.house_id = p_house_id
      AND p.id <> p_user_id;
END;
$function$;

-- get_per_diem_bill
CREATE OR REPLACE FUNCTION public.get_per_diem_bill(target_house_id uuid, target_year_month text)
 RETURNS TABLE(item_name text, rate numeric, total_quantity numeric, total_amount numeric)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    month_start date;
    month_end date;
BEGIN
    month_start := (target_year_month || '-01')::date;
    month_end := (month_start + interval '1 month' - interval '1 day')::date;

    -- Verify user has access to this house
    IF NOT EXISTS (
        SELECT 1 FROM public.house_members
        WHERE house_id = target_house_id
        AND user_id = auth.uid()
    ) THEN
        RAISE EXCEPTION 'Access denied';
    END IF;

    RETURN QUERY
    SELECT
        pdc.item_name,
        pdc.rate,
        COALESCE(SUM(pde.quantity), 0)::numeric AS total_quantity,
        (COALESCE(SUM(pde.quantity), 0) * pdc.rate)::numeric AS total_amount
    FROM public.per_diem_config pdc
    LEFT JOIN public.per_diem_entries pde ON pde.config_id = pdc.id
        AND pde.date >= month_start AND pde.date <= month_end
    WHERE pdc.house_id = target_house_id
    GROUP BY pdc.id, pdc.item_name, pdc.rate
    ORDER BY pdc.item_name;
END;
$function$;

-- get_per_diem_bill_by_member
CREATE OR REPLACE FUNCTION public.get_per_diem_bill_by_member(p_house_id uuid, p_month date)
 RETURNS TABLE(user_id uuid, full_name text, total_quantity numeric, total_amount numeric)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    IF NOT auth_is_house_member(p_house_id) THEN
        RAISE EXCEPTION 'Access denied';
    END IF;
    RETURN QUERY
    SELECT pde.added_by AS user_id, up.full_name,
        SUM(pde.quantity)::NUMERIC AS total_quantity,
        SUM(pde.quantity * pdc.rate)::NUMERIC AS total_amount
    FROM per_diem_entries pde
    JOIN per_diem_config pdc ON pdc.id = pde.config_id
    LEFT JOIN profiles up ON up.id = pde.added_by
    WHERE pdc.house_id = p_house_id
      AND DATE_TRUNC('month', pde.date) = DATE_TRUNC('month', p_month)
    GROUP BY pde.added_by, up.full_name
    ORDER BY total_amount DESC;
END;
$function$;

-- get_per_diem_bill_itemized
CREATE OR REPLACE FUNCTION public.get_per_diem_bill_itemized(p_house_id uuid, p_month date)
 RETURNS TABLE(item_name text, category text, unit text, rate numeric, total_quantity numeric, total_amount numeric)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    IF NOT auth_is_house_member(p_house_id) THEN
        RAISE EXCEPTION 'Access denied';
    END IF;
    RETURN QUERY
    SELECT pdc.item_name, pdc.category, pdc.unit, pdc.rate,
        SUM(pde.quantity)::NUMERIC AS total_quantity,
        SUM(pde.quantity * pdc.rate)::NUMERIC AS total_amount
    FROM per_diem_entries pde
    JOIN per_diem_config pdc ON pdc.id = pde.config_id
    WHERE pdc.house_id = p_house_id
      AND DATE_TRUNC('month', pde.date) = DATE_TRUNC('month', p_month)
    GROUP BY pdc.item_name, pdc.category, pdc.unit, pdc.rate
    ORDER BY total_amount DESC;
END;
$function$;

-- get_per_diem_entries_with_details
CREATE OR REPLACE FUNCTION public.get_per_diem_entries_with_details(p_house_id uuid, p_month date DEFAULT NULL::date)
 RETURNS TABLE(entry_id uuid, item_name text, category text, unit text, rate numeric, quantity numeric, total_cost numeric, date date, added_by uuid, user_name text, notes text, created_at timestamp with time zone)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    RETURN QUERY
    SELECT 
        pde.id as entry_id,
        pdc.item_name,
        pdc.category,
        pdc.unit,
        pdc.rate,
        pde.quantity,
        (pde.quantity * pdc.rate) as total_cost,
        pde.date,
        pde.added_by,
        p.full_name as user_name,
        pde.notes,
        pde.created_at
    FROM per_diem_entries pde
    JOIN per_diem_config pdc ON pdc.id = pde.config_id
    JOIN profiles p ON p.id = pde.added_by
    WHERE pdc.house_id = p_house_id
    AND (p_month IS NULL OR DATE_TRUNC('month', pde.date) = DATE_TRUNC('month', p_month))
    ORDER BY pde.date DESC, pde.created_at DESC;
END;
$function$;

-- get_recurring_expenses_with_status
CREATE OR REPLACE FUNCTION public.get_recurring_expenses_with_status(p_house_id uuid)
 RETURNS TABLE(id uuid, house_id uuid, name text, amount numeric, due_day integer, category text, created_by uuid, is_active boolean, created_at timestamp with time zone, frequency text, next_due_date date, last_paid_date date, custom_frequency_days integer, reminder_days_before integer, reminder_enabled boolean, notes text, due_status text, days_until_due integer)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
begin
    if not public.auth_is_house_member(p_house_id) then
        raise exception 'Not a member of this house' using errcode = '42501';
    end if;

    return query
    select
        re.id,
        re.house_id,
        re.name,
        re.amount,
        re.due_day,
        re.category,
        re.created_by,
        re.is_active,
        re.created_at,
        re.frequency,
        re.next_due_date,
        re.last_paid_date,
        re.custom_frequency_days,
        re.reminder_days_before,
        re.reminder_enabled,
        re.notes,
        case
            when re.next_due_date is null then 'pending'
            when re.next_due_date < current_date then 'overdue'
            when re.next_due_date = current_date then 'due_today'
            when re.next_due_date <= current_date + (re.reminder_days_before || ' days')::interval then 'upcoming'
            else 'scheduled'
        end as due_status,
        case
            when re.next_due_date is not null then (re.next_due_date - current_date)::integer
            else null
        end as days_until_due
    from public.recurring_expenses re
    where re.house_id = p_house_id
    order by
        case
            when re.next_due_date is null then 999999
            else (re.next_due_date - current_date)::integer
        end,
        re.name;
end;
$function$;

-- get_spend_by_category
CREATE OR REPLACE FUNCTION public.get_spend_by_category(p_house_id uuid, p_month date)
 RETURNS TABLE(category text, total_amount numeric)
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
DECLARE
    v_start_date DATE;
    v_end_date DATE;
BEGIN
    v_start_date := date_trunc('month', p_month)::date;
    v_end_date := (v_start_date + interval '1 month')::date;
    RETURN QUERY
    WITH monthly_category_spend AS (
        -- 1. One Time Expenses
        SELECT 
            ote.category,
            SUM(ote.amount) as amount
        FROM one_time_expenses ote
        WHERE ote.house_id = p_house_id
        AND ote.date >= v_start_date 
        AND ote.date < v_end_date
        AND ote.category != 'Settlement' -- FIX: Exclude Settlements explicitly
        GROUP BY ote.category
        UNION ALL
        -- 2. Per Diem (Category comes from Config)
        SELECT 
            pdc.category,
            SUM(pde.quantity * pdc.rate) as amount
        FROM per_diem_entries pde
        JOIN per_diem_config pdc ON pde.config_id = pdc.id
        WHERE pdc.house_id = p_house_id
        AND pde.date >= v_start_date 
        AND pde.date < v_end_date
        GROUP BY pdc.category
    )
    SELECT 
        mcs.category,
        SUM(mcs.amount)::NUMERIC as total_amount
    FROM monthly_category_spend mcs
    GROUP BY mcs.category
    ORDER BY total_amount DESC;
END;
$function$;

-- get_spend_by_member
CREATE OR REPLACE FUNCTION public.get_spend_by_member(p_house_id uuid, p_month date)
 RETURNS TABLE(user_id uuid, full_name text, total_spent numeric)
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
DECLARE
    v_start_date DATE;
    v_end_date DATE;
BEGIN
    v_start_date := date_trunc('month', p_month)::date;
    v_end_date := (v_start_date + interval '1 month')::date;
    
    RETURN QUERY
    WITH raw_spend AS (
        -- 1. Cash Outflows (What I paid initially)
        SELECT 
            ote.paid_by as uid,
            SUM(ote.amount) as amount
        FROM one_time_expenses ote
        WHERE ote.house_id = p_house_id
        AND ote.date >= v_start_date 
        AND ote.date < v_end_date
        AND ote.category != 'Settlement' -- Settlement is just money moving, not spend
        GROUP BY ote.paid_by
        
        UNION ALL
        
        -- 2. Subtract what others owe ME (Money I paid on their behalf)
        -- If I paid 100 and you owe me 50, my actual spend is 50.
        SELECT 
            ote.paid_by as uid,
            -SUM(es.amount_owed) as amount
        FROM expense_splits es
        JOIN one_time_expenses ote ON es.expense_id = ote.id
        WHERE ote.house_id = p_house_id
        AND ote.date >= v_start_date 
        AND ote.date < v_end_date
        AND ote.category != 'Settlement'
        GROUP BY ote.paid_by
        
        UNION ALL
        
        -- 3. Add what I owe OTHERS (My share of bills paid by someone else)
        -- If you paid 100 and I owe you 50, my spend increases by 50.
        SELECT 
            es.user_id as uid,
            SUM(es.amount_owed) as amount
        FROM expense_splits es
        JOIN one_time_expenses ote ON es.expense_id = ote.id
        WHERE ote.house_id = p_house_id
        AND ote.date >= v_start_date 
        AND ote.date < v_end_date
        AND ote.category != 'Settlement'
        GROUP BY es.user_id
    ),
    user_totals AS (
        SELECT 
            rs.uid as u_id,
            p.full_name as u_name,
            SUM(rs.amount)::NUMERIC as u_total
        FROM raw_spend rs
        JOIN profiles p ON rs.uid = p.id
        GROUP BY rs.uid, p.full_name
    ),
    per_diem_total AS (
        SELECT 
            SUM(pde.quantity * pdc.rate)::NUMERIC as amount
        FROM per_diem_entries pde
        JOIN per_diem_config pdc ON pde.config_id = pdc.id
        WHERE pdc.house_id = p_house_id
        AND pde.date >= v_start_date 
        AND pde.date < v_end_date
    ),
    combined_results AS (
        -- Members
        SELECT 
            ut.u_id as result_user_id,
            ut.u_name as result_full_name,
            ut.u_total as result_total_spent
        FROM user_totals ut
        
        UNION ALL
        
        -- House (Per Diem)
        SELECT 
            '00000000-0000-0000-0000-000000000000'::uuid as result_user_id,
            'House (Per Diem)'::text as result_full_name,
            COALESCE((SELECT amount FROM per_diem_total), 0) as result_total_spent
        WHERE (SELECT amount FROM per_diem_total) > 0
    )
    SELECT 
        cr.result_user_id,
        cr.result_full_name,
        cr.result_total_spent
    FROM combined_results cr
    ORDER BY cr.result_total_spent DESC;
END;
$function$;

-- get_user_balances
CREATE OR REPLACE FUNCTION public.get_user_balances(p_house_id uuid)
 RETURNS TABLE(user_id uuid, full_name text, balance numeric)
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
BEGIN
    RETURN QUERY
    SELECT 
        p.id as user_id,
        p.full_name,
        (
            -- Amount others owe me (Credits)
            COALESCE((
                SELECT SUM(es.amount_owed)
                FROM expense_splits es
                JOIN one_time_expenses ote ON es.expense_id = ote.id
                WHERE ote.house_id = p_house_id
                AND ote.paid_by = p.id
                AND es.user_id != p.id
                AND es.is_settled = false -- CRITICAL FIX
            ), 0)
            -
            -- Amount I owe others (Debts)
            COALESCE((
                SELECT SUM(es.amount_owed)
                FROM expense_splits es
                JOIN one_time_expenses ote ON es.expense_id = ote.id
                WHERE ote.house_id = p_house_id
                AND ote.paid_by != p.id
                AND es.user_id = p.id
                AND es.is_settled = false -- CRITICAL FIX
            ), 0)
        )::NUMERIC as balance
    FROM profiles p
    JOIN house_members hm ON hm.user_id = p.id
    WHERE hm.house_id = p_house_id;
END;
$function$;

-- get_user_house_ids
CREATE OR REPLACE FUNCTION public.get_user_house_ids(p_user_id uuid)
 RETURNS TABLE(house_id uuid)
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
    select hm.house_id
    from public.house_members hm
    where hm.user_id = p_user_id
      and hm.is_active = true
      and p_user_id = auth.uid()
    order by hm.joined_at desc;
$function$;

-- handle_new_user
CREATE OR REPLACE FUNCTION public.handle_new_user()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    INSERT INTO public.profiles (id, email, full_name, created_at, updated_at)
    VALUES (NEW.id, NEW.email, COALESCE(NEW.raw_user_meta_data->>'full_name', ''), now(), now())
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$function$;

-- handle_recurring_expense_dates
CREATE OR REPLACE FUNCTION public.handle_recurring_expense_dates()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
BEGIN
    -- Recalculate next_payment_date if last_payment_date changes or first_payment_date is set
    IF TG_TABLE_NAME = 'payment_history' THEN
        -- This logic might belong to a different trigger if it's on a different table,
        -- but the original update_next_payment_date was on recurring_expenses.
    ELSIF TG_TABLE_NAME = 'recurring_expenses' THEN
        -- Recalculate next_due_date when last_paid_date/frequency changes
        IF TG_OP = 'INSERT' OR (TG_OP = 'UPDATE' AND (NEW.last_paid_date IS DISTINCT FROM OLD.last_paid_date OR NEW.frequency IS DISTINCT FROM OLD.frequency)) THEN
            NEW.next_due_date := calculate_next_due_date(
                NEW.last_paid_date,
                NEW.due_day,
                NEW.frequency,
                NEW.custom_frequency_days
            );
        END IF;
    END IF;
    
    RETURN NEW;
END;
$function$;

-- handle_updated_at
CREATE OR REPLACE FUNCTION public.handle_updated_at()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$function$;

-- is_house_admin
CREATE OR REPLACE FUNCTION public.is_house_admin(p_house_id uuid, p_user_id uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
    SELECT EXISTS (
        SELECT 1
        FROM public.house_members
        WHERE house_id = p_house_id
        AND user_id = p_user_id
        AND role IN ('Owner', 'Admin')
        AND is_active = true
    );
$function$;

-- is_house_member
CREATE OR REPLACE FUNCTION public.is_house_member(p_house_id uuid, p_user_id uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
    SELECT EXISTS (
        SELECT 1
        FROM public.house_members
        WHERE house_id = p_house_id
        AND user_id = p_user_id
        AND is_active = true
    );
$function$;

-- is_house_owner
CREATE OR REPLACE FUNCTION public.is_house_owner(house_id_to_check uuid, user_id_to_check uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  SELECT EXISTS (
    SELECT 1
    FROM public.houses
    WHERE id = house_id_to_check AND owner_id = user_id_to_check
  );
$function$;

-- join_house_with_invite_code
CREATE OR REPLACE FUNCTION public.join_house_with_invite_code(code text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_house_id uuid;
    v_user_id uuid;
    v_existing_member record;
BEGIN
    -- Get current user
    v_user_id := auth.uid();
    
    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'Not authenticated');
    END IF;
    
    -- Find house by invite code
    SELECT id INTO v_house_id
    FROM houses
    WHERE invite_code = UPPER(TRIM(code));
    
    IF v_house_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'Invalid invite code');
    END IF;
    
    -- Check if user is already a member (active or inactive)
    SELECT * INTO v_existing_member
    FROM house_members
    WHERE house_id = v_house_id 
    AND user_id = v_user_id;
    
    -- KEY FIX: Handle different cases
    IF v_existing_member.id IS NOT NULL THEN
        -- User was previously a member
        IF v_existing_member.is_active = true THEN
            -- Already an active member
            RETURN jsonb_build_object('success', false, 'error', 'Already a member');
        ELSE
            -- Was inactive, reactivate them (rejoin)
            UPDATE house_members
            SET is_active = true,
                left_at = NULL,
                joined_at = NOW()  -- Update rejoined date
            WHERE house_id = v_house_id 
            AND user_id = v_user_id;
            
            RETURN jsonb_build_object('success', true, 'house_id', v_house_id, 'rejoined', true);
        END IF;
    ELSE
        -- Not a member yet, insert new membership
        INSERT INTO house_members (house_id, user_id, role, joined_at, is_active)
        VALUES (v_house_id, v_user_id, 'Member', NOW(), true);
        
        RETURN jsonb_build_object('success', true, 'house_id', v_house_id, 'rejoined', false);
    END IF;
END;
$function$;

-- leave_house
CREATE OR REPLACE FUNCTION public.leave_house(p_house_id uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_user_id UUID;
    v_is_owner BOOLEAN;
    v_member_count INTEGER;
BEGIN
    v_user_id := auth.uid();

    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'Not authenticated');
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM houses WHERE id = p_house_id AND owner_id = v_user_id
    ) INTO v_is_owner;

    IF v_is_owner THEN
        SELECT COUNT(*) INTO v_member_count
        FROM house_members
        WHERE house_id = p_house_id AND is_active = true AND user_id != v_user_id;

        IF v_member_count > 0 THEN
            RETURN jsonb_build_object(
                'success', false,
                'error', 'Owner cannot leave house with active members. Transfer ownership or remove all members first.'
            );
        END IF;
    END IF;

    UPDATE house_members
    SET is_active = false,
        left_at = NOW()
    WHERE house_id = p_house_id
      AND user_id = v_user_id;

    PERFORM log_house_activity(
        p_house_id,
        'member_left',
        v_user_id,
        jsonb_build_object('left_at', NOW())
    );

    PERFORM create_notification_for_house(
        p_house_id,
        'Member Left',
        (SELECT full_name FROM profiles WHERE id = v_user_id) || ' has left the house',
        'member_left',
        jsonb_build_object('user_id', v_user_id)::text,
        v_user_id
    );

    RETURN jsonb_build_object('success', true);
END;
$function$;

-- log_generic_activity
CREATE OR REPLACE FUNCTION public.log_generic_activity()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_house_id UUID;
    v_user_id UUID;
    v_action TEXT;
    v_details JSONB;
BEGIN
    -- Get the current user ID (may be null for system operations)
    v_user_id := auth.uid();
    
    -- Determine house_id based on table
    IF TG_TABLE_NAME = 'house_members' THEN
        v_house_id := COALESCE(NEW.house_id, OLD.house_id);
    ELSIF TG_TABLE_NAME = 'houses' THEN
        v_house_id := COALESCE(NEW.id, OLD.id);
    ELSIF TG_TABLE_NAME IN ('one_time_expenses', 'recurring_expenses', 'expense_splits', 'messages', 'chores', 'chore_assignments') THEN
        v_house_id := COALESCE(NEW.house_id, OLD.house_id);
    END IF;

    -- Determine action and build details
    IF TG_OP = 'INSERT' THEN
        v_action := TG_TABLE_NAME || '_created';
        v_details := row_to_json(NEW)::jsonb;
    ELSIF TG_OP = 'UPDATE' THEN
        v_action := TG_TABLE_NAME || '_updated';
        v_details := jsonb_build_object(
            'old', row_to_json(OLD)::jsonb,
            'new', row_to_json(NEW)::jsonb
        );
    ELSIF TG_OP = 'DELETE' THEN
        v_action := TG_TABLE_NAME || '_deleted';
        v_details := row_to_json(OLD)::jsonb;
    END IF;

    -- Only log if we have a valid house_id and user_id
    IF v_house_id IS NOT NULL AND v_user_id IS NOT NULL THEN
        INSERT INTO house_audit_log (house_id, user_id, action, details)
        VALUES (v_house_id, v_user_id, v_action, v_details);
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$function$;

-- log_house_activity
CREATE OR REPLACE FUNCTION public.log_house_activity(p_house_id uuid, p_action text, p_target_user_id uuid DEFAULT NULL::uuid, p_details jsonb DEFAULT '{}'::jsonb)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    INSERT INTO house_audit_log (house_id, user_id, action, target_user_id, details)
    VALUES (p_house_id, auth.uid(), p_action, p_target_user_id, p_details);
END;
$function$;

-- mark_all_notifications_read
CREATE OR REPLACE FUNCTION public.mark_all_notifications_read()
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    UPDATE notifications
    SET is_read = true
    WHERE user_id = auth.uid()
    AND is_read = false;
END;
$function$;

-- mark_recurring_bill_paid
CREATE OR REPLACE FUNCTION public.mark_recurring_bill_paid(p_recurring_id uuid, p_house_id uuid, p_paid_by uuid, p_name text, p_amount numeric, p_category text, p_date date, p_notes text, p_splits jsonb)
 RETURNS uuid
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
    v_house_id uuid;
    v_expense_id uuid;
    v_split jsonb;
    v_split_total numeric;
begin
    -- Authorize against the bill's actual house, not caller-supplied input.
    select house_id into v_house_id
    from recurring_expenses
    where id = p_recurring_id;

    if v_house_id is null then
        raise exception 'Recurring bill not found';
    end if;
    if v_house_id <> p_house_id then
        raise exception 'Recurring bill does not belong to the given house';
    end if;
    if not auth_is_house_member(v_house_id) then
        raise exception 'Not a member of this house';
    end if;
    if not is_house_member(v_house_id, p_paid_by) then
        raise exception 'Payer must be a member of this house';
    end if;
    if p_amount is null or p_amount <= 0 then
        raise exception 'Amount must be greater than zero';
    end if;

    select coalesce(sum((s->>'amount')::numeric), 0)
    into v_split_total
    from jsonb_array_elements(coalesce(p_splits, '[]'::jsonb)) s;

    if v_split_total < 0 or v_split_total > p_amount + 0.005 then
        raise exception 'Split amounts (%) cannot exceed the expense amount (%)', v_split_total, p_amount;
    end if;

    insert into one_time_expenses (house_id, paid_by, name, amount, category, date, notes)
    values (v_house_id, p_paid_by, p_name, p_amount, p_category, p_date, p_notes)
    returning id into v_expense_id;

    for v_split in select * from jsonb_array_elements(coalesce(p_splits, '[]'::jsonb))
    loop
        insert into expense_splits (expense_id, user_id, amount_owed)
        values (v_expense_id, (v_split->>'user_id')::uuid, (v_split->>'amount')::numeric);
    end loop;

    insert into payment_history (recurring_expense_id, paid_by, amount, payment_date)
    values (p_recurring_id, p_paid_by, p_amount, p_date);

    update recurring_expenses set last_paid_date = p_date where id = p_recurring_id;

    return v_expense_id;
end;
$function$;

-- notify_new_member
CREATE OR REPLACE FUNCTION public.notify_new_member()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_house_name TEXT;
    v_user_name TEXT;
BEGIN
    -- Only notify on INSERT (new members)
    IF TG_OP = 'INSERT' THEN
        -- Get house name
        SELECT name INTO v_house_name
        FROM houses
        WHERE id = NEW.house_id;

        -- Get user name
        SELECT full_name INTO v_user_name
        FROM profiles
        WHERE id = NEW.user_id;

        -- Notify existing members (excluding the new member)
        PERFORM create_notification_for_house(
            NEW.house_id,
            'New Member',
            COALESCE(v_user_name, 'A new member') || ' joined ' || v_house_name,
            'member_joined',
            -- FIX: Cast JSONB to TEXT
            jsonb_build_object(
                'user_id', NEW.user_id,
                'user_name', COALESCE(v_user_name, 'New member'),
                'house_id', NEW.house_id
            )::text,
            NEW.user_id  -- Exclude the new member
        );
    END IF;

    RETURN NEW;
END;
$function$;

-- prevent_expense_house_change
CREATE OR REPLACE FUNCTION public.prevent_expense_house_change()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
BEGIN
    IF NEW.house_id IS DISTINCT FROM OLD.house_id THEN
        RAISE EXCEPTION 'house_id cannot be changed';
    END IF;
    RETURN NEW;
END;
$function$;

-- prevent_self_owing_splits
CREATE OR REPLACE FUNCTION public.prevent_self_owing_splits()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
DECLARE
    payer_id uuid;
BEGIN
    -- Get the payer of this expense
    SELECT paid_by INTO payer_id
    FROM one_time_expenses
    WHERE id = NEW.expense_id;

    -- If the split is for the payer themselves, skip the insert silently
    IF NEW.user_id = payer_id THEN
        RETURN NULL;
    END IF;

    RETURN NEW;
END;
$function$;

-- process_recurring_bill_split
CREATE OR REPLACE FUNCTION public.process_recurring_bill_split(p_expense_id uuid, p_payer_id uuid, p_amount numeric)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_expense RECORD;
    v_house_id UUID;
    v_split_count INTEGER;
    v_split_amount NUMERIC;
    v_user_id UUID;
    v_custom_amount NUMERIC;
BEGIN
    -- Get expense details
    SELECT * INTO v_expense
    FROM recurring_expenses
    WHERE id = p_expense_id;
    
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Expense not found');
    END IF;
    
    v_house_id := v_expense.house_id;
    
    -- If no split configured, just record payment
    IF v_expense.split_with IS NULL OR array_length(v_expense.split_with, 1) = 0 THEN
        RETURN jsonb_build_object('success', true, 'splits_created', 0);
    END IF;
    
    -- Process based on split type
    IF v_expense.split_type = 'equal' THEN
        -- Equal split
        v_split_count := array_length(v_expense.split_with, 1);
        v_split_amount := p_amount / v_split_count;
        
        -- Create expense splits for each member (excluding payer)
        FOR v_user_id IN SELECT unnest(v_expense.split_with)
        LOOP
            IF v_user_id != p_payer_id THEN
                INSERT INTO expense_splits (expense_id, user_id, amount_owed, created_at)
                VALUES (p_expense_id, v_user_id, v_split_amount, NOW())
                ON CONFLICT DO NOTHING;
            END IF;
        END LOOP;
        
    ELSIF v_expense.split_type = 'custom' AND v_expense.split_amounts IS NOT NULL THEN
        -- Custom split
        FOR v_user_id IN SELECT unnest(v_expense.split_with)
        LOOP
            IF v_user_id != p_payer_id THEN
                -- Get custom amount from JSONB
                v_custom_amount := (v_expense.split_amounts->v_user_id::TEXT)::NUMERIC;
                
                IF v_custom_amount IS NOT NULL AND v_custom_amount > 0 THEN
                    INSERT INTO expense_splits (expense_id, user_id, amount_owed, created_at)
                    VALUES (p_expense_id, v_user_id, v_custom_amount, NOW())
                    ON CONFLICT DO NOTHING;
                END IF;
            END IF;
        END LOOP;
    END IF;
    
    RETURN jsonb_build_object('success', true, 'splits_created', v_split_count);
END;
$function$;

-- regenerate_invite_code
CREATE OR REPLACE FUNCTION public.regenerate_invite_code(p_house_id uuid)
 RETURNS text
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_new_code TEXT;
BEGIN
    -- Verify user is house admin (Normalized roles)
    IF NOT EXISTS (
        SELECT 1 FROM public.house_members
        WHERE house_id = p_house_id
          AND user_id = auth.uid()
          AND role IN ('Owner', 'Admin')
          AND is_active = true
    ) THEN
        RAISE EXCEPTION 'Access denied: Only house admins can regenerate invite codes';
    END IF;

    v_new_code := generate_invite_code();

    UPDATE public.houses
    SET invite_code = v_new_code,
        invite_code_generated_at = NOW(),
        invite_code_expires_at = NOW() + INTERVAL '90 days'
    WHERE id = p_house_id;

    PERFORM log_house_activity(
        p_house_id,
        'invite_code_regenerated',
        NULL,
        jsonb_build_object('new_code', v_new_code)
    );

    RETURN v_new_code;
END;
$function$;

-- remove_house_member
CREATE OR REPLACE FUNCTION public.remove_house_member(p_house_id uuid, p_user_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    IF NOT auth_is_house_admin(p_house_id) THEN
        RAISE EXCEPTION 'Not authorized to remove members from this house';
    END IF;
    IF is_house_owner(p_house_id, p_user_id) THEN
        RAISE EXCEPTION 'Cannot remove the house owner';
    END IF;
    DELETE FROM house_members
    WHERE house_id = p_house_id AND user_id = p_user_id;
END;
$function$;

-- resend_invitation
CREATE OR REPLACE FUNCTION public.resend_invitation(p_house_id uuid, p_email text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_user_id uuid;
  v_house_name text;
begin
  -- Check permissions (Normalized roles)
  if not exists (
    select 1 from public.house_members
    where house_id = p_house_id 
    and user_id = auth.uid()
    and role in ('Owner', 'Admin')
    and is_active = true
  ) then
    raise exception 'Insufficient permissions to resend invitations';
  end if;

  select id into v_user_id from public.profiles where email = p_email;
  select name into v_house_name from public.houses where id = p_house_id;

  if v_user_id is not null then
    insert into public.notifications (user_id, house_id, title, message, type, is_read)
    values (v_user_id, p_house_id, 'House Invitation', 'You have been invited to join ' || v_house_name, 'house_invitation', false);
  end if;
end;
$function$;

-- set_invite_code
CREATE OR REPLACE FUNCTION public.set_invite_code()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    IF NEW.invite_code IS NULL THEN
        NEW.invite_code := generate_invite_code();
    END IF;
    RETURN NEW;
END;
$function$;

-- set_owner_role
CREATE OR REPLACE FUNCTION public.set_owner_role()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
BEGIN
    IF EXISTS (SELECT 1 FROM houses WHERE id = NEW.house_id AND owner_id = NEW.user_id) THEN
        NEW.role = 'Owner';
    ELSE
        -- Always clamp on insert; never trust a client-supplied role.
        NEW.role = 'Member';
    END IF;
    RETURN NEW;
END;
$function$;

-- settle_balance
CREATE OR REPLACE FUNCTION public.settle_balance(p_house_id uuid, p_payer_id uuid, p_payee_id uuid, p_amount numeric, p_description text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_expense_id uuid;
BEGIN
    IF NOT auth_is_house_member(p_house_id) THEN
        RAISE EXCEPTION 'Not a member of this house';
    END IF;
    IF auth.uid() <> p_payer_id AND auth.uid() <> p_payee_id THEN
        RAISE EXCEPTION 'You can only settle balances you are part of';
    END IF;
    IF p_amount IS NULL OR p_amount <= 0 THEN
        RAISE EXCEPTION 'Settlement amount must be greater than zero';
    END IF;

    INSERT INTO transactions (house_id, payer_id, payee_id, amount, is_settlement, description)
    VALUES (p_house_id, p_payer_id, p_payee_id, p_amount, true, p_description);

    INSERT INTO one_time_expenses (house_id, paid_by, name, amount, category, date, notes)
    VALUES (p_house_id, p_payer_id, 'Settlement', p_amount, 'Settlement', CURRENT_DATE, p_description)
    RETURNING id INTO v_expense_id;

    INSERT INTO expense_splits (expense_id, user_id, amount_owed, is_settled)
    VALUES (v_expense_id, p_payee_id, p_amount, true);
END;
$function$;

-- settle_expense_splits
CREATE OR REPLACE FUNCTION public.settle_expense_splits()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_remaining NUMERIC;
    r RECORD;
BEGIN
    IF NEW.is_settlement = true THEN
        v_remaining := COALESCE(NEW.amount, 0);

        FOR r IN
            SELECT es.id AS split_id, es.expense_id, es.amount_owed
            FROM expense_splits es
            JOIN one_time_expenses ote ON ote.id = es.expense_id
            WHERE es.user_id = NEW.payer_id            -- the person who owed
              AND ote.paid_by = NEW.payee_id           -- the person who was owed
              AND ote.house_id = NEW.house_id
              AND es.is_settled = false
              AND ote.category <> 'Settlement'         -- never re-settle settlement rows
            ORDER BY ote.date ASC, es.id ASC           -- oldest debt first
        LOOP
            EXIT WHEN v_remaining <= 0;

            IF r.amount_owed <= v_remaining THEN
                UPDATE expense_splits SET is_settled = true WHERE id = r.split_id;
                v_remaining := v_remaining - r.amount_owed;
            ELSE
                -- Partial: reduce the outstanding amount on this split and record the paid
                -- portion as a separate settled row so history/totals stay consistent.
                UPDATE expense_splits
                SET amount_owed = amount_owed - v_remaining
                WHERE id = r.split_id;

                INSERT INTO expense_splits (expense_id, user_id, amount_owed, is_settled)
                VALUES (r.expense_id, NEW.payer_id, v_remaining, true);

                v_remaining := 0;
            END IF;
        END LOOP;
    END IF;

    RETURN NEW;
END;
$function$;

-- shares_house_with
CREATE OR REPLACE FUNCTION public.shares_house_with(profile_user_id uuid, requesting_user_id uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
  SELECT EXISTS (
    SELECT 1 
    FROM house_members hm1
    JOIN house_members hm2 ON hm1.house_id = hm2.house_id
    WHERE hm1.user_id = requesting_user_id
    AND hm2.user_id = profile_user_id
  );
$function$;

-- transfer_house_ownership
CREATE OR REPLACE FUNCTION public.transfer_house_ownership(p_house_id uuid, p_new_owner_id uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_current_owner_id UUID;
    v_new_owner_name TEXT;
BEGIN
    SELECT owner_id INTO v_current_owner_id
    FROM houses
    WHERE id = p_house_id;

    IF v_current_owner_id != auth.uid() THEN
        RETURN jsonb_build_object('success', false, 'error', 'Only the owner can transfer ownership');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM house_members
        WHERE house_id = p_house_id AND user_id = p_new_owner_id AND is_active = true
    ) THEN
        RETURN jsonb_build_object('success', false, 'error', 'New owner must be an active member');
    END IF;

    SELECT full_name INTO v_new_owner_name
    FROM profiles
    WHERE id = p_new_owner_id;

    UPDATE houses
    SET owner_id = p_new_owner_id
    WHERE id = p_house_id;

    -- Promote the new owner BEFORE demoting the current owner, so the role-change guard
    -- (which requires the caller to still be an admin) never rejects the second update.
    UPDATE house_members
    SET role = 'Owner'
    WHERE house_id = p_house_id AND user_id = p_new_owner_id;

    UPDATE house_members
    SET role = 'Member'
    WHERE house_id = p_house_id AND user_id = v_current_owner_id;

    PERFORM log_house_activity(
        p_house_id,
        'ownership_transferred',
        p_new_owner_id,
        jsonb_build_object(
            'old_owner_id', v_current_owner_id,
            'new_owner_id', p_new_owner_id,
            'new_owner_name', v_new_owner_name
        )
    );

    PERFORM create_notification_for_house(
        p_house_id,
        'Ownership Transferred',
        v_new_owner_name || ' is now the owner of this house',
        'ownership_transferred',
        jsonb_build_object('new_owner_id', p_new_owner_id)::text
    );

    RETURN jsonb_build_object('success', true);
END;
$function$;

-- update_house_config_updated_at
CREATE OR REPLACE FUNCTION public.update_house_config_updated_at()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$function$;

-- update_member_activity
CREATE OR REPLACE FUNCTION public.update_member_activity(p_house_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
BEGIN
    UPDATE house_members
    SET last_activity_at = NOW()
    WHERE house_id = p_house_id
      AND user_id = auth.uid();
END;
$function$;

-- update_next_payment_date
CREATE OR REPLACE FUNCTION public.update_next_payment_date()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
BEGIN
    -- When last_payment_date is updated, recalculate next_payment_date
    IF NEW.last_payment_date IS DISTINCT FROM OLD.last_payment_date THEN
        NEW.next_payment_date := calculate_next_payment_date(
            NEW.frequency,
            NEW.last_payment_date,
            NEW.first_payment_date
        );
    END IF;
    
    RETURN NEW;
END;
$function$;

-- update_one_time_expense
CREATE OR REPLACE FUNCTION public.update_one_time_expense(p_expense_id uuid, p_name text, p_amount numeric, p_category text, p_date date, p_notes text, p_splits jsonb)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
    v_house_id uuid;
    v_paid_by uuid;
    v_amount numeric;
    v_split_total numeric;
begin
    select house_id, paid_by, amount
    into v_house_id, v_paid_by, v_amount
    from one_time_expenses
    where id = p_expense_id;

    if v_house_id is null then
        raise exception 'Expense not found';
    end if;
    if not auth_is_house_member(v_house_id) then
        raise exception 'Not a member of this house';
    end if;
    if p_amount is not null and p_amount <= 0 then
        raise exception 'Amount must be greater than zero';
    end if;

    -- Update only provided fields; null means "leave unchanged".
    update one_time_expenses set
        name = coalesce(p_name, name),
        amount = coalesce(p_amount, amount),
        category = coalesce(p_category, category),
        date = coalesce(p_date, date),
        notes = coalesce(p_notes, notes)
    where id = p_expense_id;

    -- Replace splits atomically only when a split set was supplied.
    if p_splits is not null then
        select coalesce(sum((s->>'amount')::numeric), 0)
        into v_split_total
        from jsonb_array_elements(p_splits) s
        where (s->>'user_id')::uuid <> v_paid_by;

        if v_split_total < 0 or v_split_total > coalesce(p_amount, v_amount) + 0.005 then
            raise exception 'Split amounts (%) cannot exceed the expense amount (%)',
                v_split_total, coalesce(p_amount, v_amount);
        end if;

        delete from expense_splits where expense_id = p_expense_id;

        insert into expense_splits (expense_id, user_id, amount_owed)
        select p_expense_id, (s->>'user_id')::uuid, (s->>'amount')::numeric
        from jsonb_array_elements(p_splits) s
        where (s->>'user_id')::uuid <> v_paid_by;
    end if;
end;
$function$;

-- update_recurring_expense_next_due
CREATE OR REPLACE FUNCTION public.update_recurring_expense_next_due()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
BEGIN
    -- Calculate next due date when last_paid_date is updated
    IF NEW.last_paid_date IS NOT NULL AND (OLD.last_paid_date IS NULL OR NEW.last_paid_date != OLD.last_paid_date) THEN
        NEW.next_due_date := calculate_next_due_date(
            NEW.last_paid_date,
            NEW.frequency,
            NEW.due_day,
            NEW.custom_frequency_days
        );
    END IF;

    -- If next_due_date is null and expense is active, calculate it
    IF NEW.next_due_date IS NULL AND NEW.is_active = true THEN
        NEW.next_due_date := calculate_next_due_date(
            NEW.last_paid_date,
            NEW.frequency,
            NEW.due_day,
            NEW.custom_frequency_days
        );
    END IF;

    RETURN NEW;
END;
$function$;

-- update_recurring_expense_next_due_date
CREATE OR REPLACE FUNCTION public.update_recurring_expense_next_due_date()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
BEGIN
    -- Update next_due_date when last_paid_date changes or on insert
    IF TG_OP = 'INSERT' OR (TG_OP = 'UPDATE' AND (NEW.last_paid_date IS DISTINCT FROM OLD.last_paid_date OR NEW.frequency IS DISTINCT FROM OLD.frequency)) THEN
        NEW.next_due_date := calculate_next_due_date(
            NEW.last_paid_date,
            NEW.due_day,
            NEW.frequency,
            NEW.custom_frequency_days
        );
    END IF;

    RETURN NEW;
END;
$function$;

-- update_updated_at_column
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$function$;
