package com.ecommerce.paymentservice.inbox.entity;

import com.ecommerce.common.entity.BaseInbox;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "inbox")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Inbox extends BaseInbox {
}
