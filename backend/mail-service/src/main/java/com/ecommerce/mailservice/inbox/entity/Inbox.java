package com.ecommerce.mailservice.inbox.entity;

import com.ecommerce.common.entity.BaseInbox;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "inbox")
@SuperBuilder
@NoArgsConstructor
public class Inbox extends BaseInbox {
}
