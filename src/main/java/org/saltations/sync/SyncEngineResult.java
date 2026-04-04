package org.saltations.sync;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated result for syncing all subscriptions on a project in one call.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncEngineResult {

    private List<SyncSubscriptionResult> subscriptionResults = new ArrayList<>();
}
