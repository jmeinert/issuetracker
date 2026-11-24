import { createTheme, Input, Notification } from '@mantine/core';

export const theme = createTheme({
  defaultRadius: 'xl',
  components: {
    InputWrapper: Input.Wrapper.extend({
      defaultProps: {
        withAsterisk: false,
      },
    }),
    Notification: Notification.extend({
      defaultProps: {
        closeButtonProps: {
          'aria-label': 'Dismiss notification',
        },
      },
    }),
  },
});
